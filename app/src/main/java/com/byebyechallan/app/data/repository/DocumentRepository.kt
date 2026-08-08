package com.byebyechallan.app.data.repository

import com.byebyechallan.app.data.model.*
import com.byebyechallan.app.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DocumentRepository(private val api: ApiService) {

    /** Fetches the required-document checklist for a vehicle/registration combination. */
    suspend fun getDocumentChecklist(
        country: String,
        state: String,
        registrationType: String,
        vehicleType: String
    ): ApiResult<List<DocumentRequestDto>> {
        return try {
            val response = api.getDocumentList(country, state, registrationType, vehicleType)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Couldn't load the document checklist (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading document checklist.")
        }
    }

    /** Fetches documents already uploaded for this vehicle. */
    suspend fun getUploadedDocuments(
        userId: Long,
        profileId: Long,
        vehicleRegistrationNo: String
    ): ApiResult<List<UserDocumentDto>> {
        return try {
            val response = api.getAllDocuments(userId, profileId, vehicleRegistrationNo)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Couldn't load uploaded documents (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while loading documents.")
        }
    }

    /**
     * Merges the checklist (what's required) with what's already uploaded,
     * so the Vehicle Detail screen can show one combined list with status badges.
     */
    fun mergeChecklist(
        templates: List<DocumentRequestDto>,
        uploaded: List<UserDocumentDto>
    ): List<DocumentChecklistItem> {
        return templates.map { template ->
            val templateKey = template.docTemplateId.ifBlank { template.docId }
            val match = uploaded.find {
                it.docTemplateId == templateKey ||
                    it.docTemplateId == template.docId ||
                    it.docTemplateId == template.docTemplateId
            }
            DocumentChecklistItem(
                template = template.copy(
                    docTemplateId = templateKey,
                    docId = templateKey.ifBlank { template.docId }
                ),
                uploaded = match
            )
        }
    }

    /**
     * Step 1 of upload flow: send the raw file to the backend's multipart endpoint.
     */
    suspend fun uploadFile(userId: Long, file: File): ApiResult<FileResponseDto> {
        return try {
            val requestFile = file.asRequestBody("*/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.uploadDocumentFile(userId, filePart)
            val body = response.body()
            if (response.isSuccessful && body != null && body.storedFileName != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("File upload failed (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while uploading file.")
        }
    }

    /** Step 2 of upload flow: save the document record (with fileName from step 1). */
    suspend fun saveDocument(
        userId: Long,
        profileId: Long,
        vehicleRegistrationNo: String,
        request: DocumentRequestDto
    ): ApiResult<UserDocumentDto> {
        return try {
            val response = api.saveDocument(userId, profileId, vehicleRegistrationNo, request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Couldn't save document (${response.code()}).")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error while saving document.")
        }
    }
}
