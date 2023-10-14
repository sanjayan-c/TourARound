package com.example.touraround

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//@OptIn(markerClass = arrayOf(androidx.camera.core.ExperimentalGetImage::class))
class Translator : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var recognizedTextView: TextView
    private lateinit var capturedImageView: ImageView
    private lateinit var functions: FirebaseFunctions
    private lateinit var transTextView: TextView
    private lateinit var languageSpinner: Spinner
    private lateinit var selectedLanguageCode: String
    private lateinit var backInTopBar : ImageView
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userId = firebaseUser?.uid
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translator)
        viewFinder = findViewById(R.id.viewfinder)
        cameraExecutor = Executors.newSingleThreadExecutor()
        transTextView = findViewById(R.id.transTextView)
        recognizedTextView = findViewById(R.id.recognizedTextView)
        capturedImageView = findViewById(R.id.capturedImageView)
        capturedImageView.visibility = View.GONE
        languageSpinner = findViewById(R.id.languageSpinner)
        selectedLanguageCode = ""

        // Retrieve the location from the intent
        val currentLocation = intent.getParcelableExtra<Location>("currentLocation")

        if (currentLocation != null) {
            // Log the location data
            Log.d("Translator", "Latitude: ${currentLocation.latitude}, Longitude: ${currentLocation.longitude}")
        }

        val supportedLanguages = getSupportedLanguages()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            supportedLanguages.map { it.getDisplayName(Locale.getDefault()) }
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = supportedLanguages[position]
                selectedLanguageCode = selectedLanguage.language
                Log.d("LanguageSpinner", "Selected Language Code: $selectedLanguageCode")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        backInTopBar = findViewById(R.id.backInTopBar)
        backInTopBar.visibility = View.VISIBLE
        backInTopBar.setOnClickListener{
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }
        val userMenu = findViewById<ImageView>(R.id.userMenu)

        // Set up a click listener for the ImageView
        userMenu.setOnClickListener { v ->
            // Create a PopupMenu
            val popupMenu = PopupMenu(this, v)
            popupMenu.inflate(R.menu.user_menu) // Use your custom menu XML
            val item2 = popupMenu.menu.findItem(R.id.menu_item2)
            item2.isVisible = false
            if(userId==null){
                val item1 = popupMenu.menu.findItem(R.id.menu_item1)
                item1.isVisible = false
            }
            // Set a listener for menu item clicks
            popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item1 -> {
                        // Handle the click on menu_item1
                        val intent = Intent(this@Translator, ProfileActivity::class.java) // Replace YourActivity with the desired activity
                        startActivity(intent)
                        // Add your custom logic here
                        return@OnMenuItemClickListener true
                    }

                    R.id.menu_item3 -> {
                        // Handle the click on menu_item3
                        // Create an AlertDialog for logout confirmation
                        val alertDialogBuilder = AlertDialog.Builder(this)

                        // Set the dialog title and message for logout confirmation
                        alertDialogBuilder
                            .setTitle("Log Out")
                            .setMessage("Are you sure you want to log out?")

                        // Add a "Cancel" button
                        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                            // Dismiss the dialog if "Cancel" is clicked
                            dialog.dismiss()
                        }

                        // Add a "Log Out" button
                        alertDialogBuilder.setPositiveButton("Log Out") { dialog, _ ->
                            // Perform the logout action
                            FirebaseAuth.getInstance().signOut()
                            // Start the CustomerLogIn activity
                            val intent = Intent(this@Translator, Login::class.java)
                            finish()
                            startActivity(intent)
                            // Dismiss the dialog
                            dialog.dismiss()
                        }

                        // Create and show the AlertDialog
                        val alertDialog = alertDialogBuilder.create()
                        alertDialog.show()

                        return@OnMenuItemClickListener true
                    }
                    // Add more menu items as needed
                    else -> false
                }
            })

            // Show the PopupMenu
            popupMenu.show()
        }


    }

    private fun getSupportedLanguages(): List<Locale> {
        return listOf(
            Locale("en"),
            Locale("es"),
            Locale("fr"),
            Locale("de"),
            Locale("ja"),
            Locale("ko"),
            Locale("zh", "CN"),
            Locale("ru"),
            Locale("ar"),
            Locale("hi"),
            Locale("pt"),
            Locale("it"),
            Locale("ta"),
            Locale("si")
        )
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Get the PreviewView from the layout
            val previewView = findViewById<PreviewView>(R.id.viewfinder)

            // Create a Preview instance
            val preview = Preview.Builder().build()

            // Set the surface provider for the preview to the PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Create a CameraSelector for the default back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Build the ImageCapture instance
            imageCapture = ImageCapture.Builder().build()

            // Bind the camera to the lifecycle with the specified components
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )

        }, ContextCompat.getMainExecutor(this))
    }

    fun captureImage(view: View) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageToBitmap(image)

                    if (bitmap != null) {
                        capturedImageView.visibility = View.VISIBLE
                        capturedImageView.setImageBitmap(bitmap)
                        recognizeText(bitmap)
                    }
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = "Capture failed: ${exception.message}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)

        try {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bit = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            recognizeText(bit)
            return bit
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageToBitmap", "Failed to decode image: ${e.message}")
            return null
        }
    }

    private fun recognizeText(bitmap: Bitmap) {
        val bitmap = scaleBitmapDown(bitmap, 640)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

   //     functions = Firebase.functions

        val request = JsonObject()
        try {
            val image = JsonObject()
            image.add("content", JsonPrimitive(base64encoded))
            request.add("image", image)

            val feature = JsonObject()
            feature.add("type", JsonPrimitive("TEXT_DETECTION"))
            val features = JsonArray()
            features.add(feature)
            request.add("features", features)

            // Add the selected language code to the request
            val imageContext = JsonObject()
            imageContext.add("languageHints", JsonArray().apply { add(JsonPrimitive(selectedLanguageCode)) })
            request.add("imageContext", imageContext)

            val requests = JsonArray()
            requests.add(request)

            val imageRequest = JsonObject()
            imageRequest.add("requests", requests)

            Log.d("FirebaseOCR", "Request prepared: $imageRequest")

            val builder = OkHttpClient.Builder()
            val client: OkHttpClient = builder.build()
            val body: RequestBody =
                imageRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request: Request = Request.Builder()
                .url("https://vision.googleapis.com/v1/images:annotate?key=AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c")
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle this
                }

                override fun onResponse(call: Call, response: Response) {
                    System.out.format("%nComplete annotation:")
                    System.out.format("%n%s", response.message)
                    Log.d(ContentValues.TAG, "Response: ${response.message}")

                    val finalText = response.body?.string()
                    Log.d(ContentValues.TAG, "OCRED TEXT: $finalText")

                    try {
                        val json = JSONObject(finalText)
                        val responses = json.getJSONArray("responses")

                        if (responses.length() > 0) {
                            val firstResponse = responses.getJSONObject(0)

                            val textAnnotations = firstResponse.getJSONArray("textAnnotations")

                            val fullTextAnnotation = firstResponse.getJSONObject("fullTextAnnotation")
                            val languageCode = fullTextAnnotation.optString("locale")
                            Log.d("FirebaseOCR", "Language Code: $languageCode")

                            val extractedDescriptions = mutableListOf<String>()

                            for (i in 1 until textAnnotations.length()) {
                                val annotation = textAnnotations.getJSONObject(i)
                                val description = annotation.getString("description")
                                Log.d("FirebaseOCR", "Extracted Text: $description")
                                extractedDescriptions.add(description)
                            }

                            val joinedDescriptions = extractedDescriptions.joinToString(" ")

                            runOnUiThread {
                                recognizedTextView.text = "$joinedDescriptions"
                            }

                            val targetLang = selectedLanguageCode // for Arabic
                            val translatedText =
                                translateText("AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c", joinedDescriptions, targetLang)
                            println("Translated Text: $translatedText")
                            runOnUiThread {
                                transTextView.text = "$translatedText"
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("FirebaseOCR", "Error parsing JSON: ${e.message}", e)
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("FirebaseOCR", "Error preparing OCR request: ${e.message}", e)
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    fun translateText(apiKey: String, sourceText: String, targetLanguage: String): String {
        val translate = TranslateOptions.newBuilder().setApiKey(apiKey).build().service
        val translation = translate.translate(
            sourceText,
            Translate.TranslateOption.targetLanguage(targetLanguage)
        )
        return translation.translatedText
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
