package com.example.vougecycle

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.vougecycle.databinding.ActivityEditProfileBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

class EditProfileActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null
    private var existingAvatarUrl: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivAvatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnChangePhoto.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSave.setOnClickListener { saveProfile() }
        loadProfile()
    }

    private fun loadProfile() {
        val uid = Supabase.client.auth.currentUserOrNull()?.id ?: return
        binding.progressProfile.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val profile = Supabase.client.postgrest.from("profiles").select()
                    .decodeList<Profile>().firstOrNull { it.id == uid } ?: return@launch
                binding.etFullName.setText(profile.fullName)
                binding.etUsername.setText(profile.username)
                binding.etLocation.setText(profile.location.orEmpty())
                binding.etBio.setText(profile.bio.orEmpty())
                existingAvatarUrl = profile.avatarUrl
                binding.ivAvatar.load(profile.avatarUrl) {
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } catch (e: Exception) {
                LanguageManager.makeToast(this@EditProfileActivity, AppReliability.friendlyMessage(e, "load profile"), Toast.LENGTH_LONG).show()
            } finally {
                binding.progressProfile.visibility = View.GONE
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim().removePrefix("@")
        val location = binding.etLocation.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (fullName.length < 2) {
            binding.etFullName.error = "Enter your full name"
            return
        }
        if (!username.matches(Regex("[A-Za-z0-9_]{3,30}"))) {
            binding.etUsername.error = "Use 3–30 letters, numbers or underscores"
            return
        }
        if (location.length < 2) {
            binding.etLocation.error = "Enter your town or province"
            return
        }
        if (bio.length > 160) {
            binding.etBio.error = "Bio must be 160 characters or less"
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."
        lifecycleScope.launch {
            try {
                val avatarUrl = selectedImageUri?.let { uploadAvatar(it) } ?: existingAvatarUrl
                val uid = Supabase.client.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("You must be logged in")
                val update = buildJsonObject {
                    put("full_name", JsonPrimitive(fullName))
                    put("username", JsonPrimitive(username))
                    put("location", JsonPrimitive(location))
                    put("bio", JsonPrimitive(bio))
                    if (avatarUrl != null) put("avatar_url", JsonPrimitive(avatarUrl))
                }
                Supabase.client.postgrest.from("profiles").update(update) {
                    filter { eq("id", uid) }
                }
                LanguageManager.makeToast(this@EditProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save profile"
                val message = if (e.message?.contains("unique", ignoreCase = true) == true) {
                    "That username is already taken"
                } else AppReliability.friendlyMessage(e, "save profile")
                LanguageManager.makeToast(this@EditProfileActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun uploadAvatar(uri: Uri): String {
        val uid = Supabase.client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("You must be logged in")
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val path = "$uid/${UUID.randomUUID()}.$extension"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read selected photo")
        require(bytes.size <= 5 * 1024 * 1024) { "Profile photo must be smaller than 5 MB" }
        Supabase.client.storage.from("profile-images").upload(path, bytes) {
            upsert = false
            contentType = ContentType.parse(mimeType)
        }
        return Supabase.client.storage.from("profile-images").publicUrl(path)
    }
}
