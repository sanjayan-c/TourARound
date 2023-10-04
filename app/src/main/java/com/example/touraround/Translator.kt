package com.example.touraround

import android.widget.ArrayAdapter
import android.widget.Spinner
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
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
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import java.util.Locale


@OptIn(markerClass = arrayOf(androidx.camera.core.ExperimentalGetImage::class))
class Translator: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var recognizedTextView: TextView
    private lateinit var capturedImageView: ImageView
    private lateinit var functions: FirebaseFunctions
    private lateinit var transTextView: TextView
    private lateinit var languageSpinner: Spinner
//    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
private lateinit var languageCode: String



    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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


        val supportedLanguages = getSupportedLanguages()

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            supportedLanguages.map { it.getDisplayName(Locale.getDefault()) }
        )

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        languageSpinner.adapter = adapter

        // Set a listener for item selection
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = supportedLanguages[position]
                 languageCode = selectedLanguage.language


                // Handle the selected language as needed
                Log.d("LanguageSpinner", "Selected Language Code: $languageCode")
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
//        val logButton = findViewById<Button>(R.id.logButton)
//        logButton.setOnClickListener {
//            // Log the event with parameters
//        }
    }



    private fun getSupportedLanguages(): List<Locale> {
        // Replace this with your code to fetch the list of supported languages from the Translation API
        // You might want to use the Cloud Translation API to get the list dynamically
        // For simplicity, I'll provide a hardcoded list here
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
            Locale("it")
        )
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val previewView = findViewById<PreviewView>(R.id.viewfinder)
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder().build()

//            val imageAnalysis = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//
//            // Attach the YourImageAnalyzer to the ImageAnalysis use case
//            imageAnalysis.setAnalyzer(cameraExecutor, YourImageAnalyzer())

            preview.setSurfaceProvider(previewView.surfaceProvider)

            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )

        }, ContextCompat.getMainExecutor(this))
    }

//    @ExperimentalGetImage private inner class YourImageAnalyzer : ImageAnalysis.Analyzer {
//        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
//            0 -> FirebaseVisionImageMetadata.ROTATION_0
//            90 -> FirebaseVisionImageMetadata.ROTATION_90
//            180 -> FirebaseVisionImageMetadata.ROTATION_180
//            270 -> FirebaseVisionImageMetadata.ROTATION_270
//            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
//        }
//
//        override fun analyze(imageProxy: ImageProxy) {
//            val mediaImage = imageProxy.image
//            val rotation = degreesToFirebaseRotation(imageProxy.imageInfo.rotationDegrees)
//
//            if (mediaImage != null) {
//                val image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation)
//
//                val detector = FirebaseVision.getInstance().cloudTextRecognizer
//// Or, to change the default settings:
//
//                val result = detector.processImage(image)
//                    .addOnSuccessListener { firebaseVisionText ->
//                        // Task completed successfully
//                        val resultText = firebaseVisionText.text
//                        recognizedTextView.text = resultText
//                        // ...
//                    }
//                    .addOnFailureListener { e ->
//                        // Task failed with an exception
//                        // ...
//                    }
//
//
////                val resultText = result.text
////
////                for (block in result.textBlocks) {
////                    val blockText = block.text
////                    val blockConfidence = block.confidence
////                    val blockLanguages = block.recognizedLanguages
////                    val blockCornerPoints = block.cornerPoints
////                    val blockFrame = block.boundingBox
////                    for (line in block.lines) {
////                        val lineText = line.text
////                        val lineConfidence = line.confidence
////                        val lineLanguages = line.recognizedLanguages
////                        val lineCornerPoints = line.cornerPoints
////                        val lineFrame = line.boundingBox
////                        for (element in line.elements) {
////                            val elementText = element.text
////                            val elementConfidence = element.confidence
////                            val elementLanguages = element.recognizedLanguages
////                            val elementCornerPoints = element.cornerPoints
////                            val elementFrame = element.boundingBox
////                        }
////                    }
////                }
//            }
//        }
//    }





    fun captureImage(view: View) {
        imageCapture.takePicture(

            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageToBitmap(image)
//                    val imageAnalysis = ImageAnalysis.Builder()
//                   .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                   .build()
//                    imageAnalysis.setAnalyzer(cameraExecutor, YourImageAnalyzer())


                    if (bitmap != null) {
                        capturedImageView.visibility = View.VISIBLE
                        capturedImageView.setImageBitmap(bitmap)
//                        val text = recognizeText(bitmap).toString()
//                        Log.d("recognized text", "$text")
//                        // recognizedTextView.text=text
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
            // Attempt to decode the byte array into a Bitmap
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bit = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
//            val text = recognizeText(bit).toString()
//            Log.d("recognized text", "$text")
            recognizeText(bit)
            return bit
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ImageToBitmap", "Failed to decode image: ${e.message}")
            return null
        }
    }


//    private fun recoText(bitmap: Bitmap) {
//        val image = FirebaseVisionImage.fromBitmap(bitmap)
//        val recognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
//
//        recognizer.processImage(image)
//            .addOnSuccessListener { visionText ->
//                // Task completed successfully
//                val recognizedText = visionText.text
//                recognizedTextView.text = recognizedText
////                translateWord(recognizedText)
//            }
//            .addOnFailureListener { e ->
//                // Task failed with an exception
//                // Handle the failure or display an error message if needed
//                Log.e("TextRecognition", "Text recognition failed: ${e.message}", e)
//                recognizedTextView.text = "Text recognition failed: ${e.message}"
//
//            }
//    }




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





    private fun recognizeText(bitmap: Bitmap) {

        // Scale down bitmap size
        val bitmap = scaleBitmapDown(bitmap, 640)

        // Convert bitmap to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        functions = Firebase.functions


        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        try {
            val image = JsonObject()
            image.add("content", JsonPrimitive(base64encoded))
            request.add("image", image)

            // Add features to the request
            val feature = JsonObject()
            feature.add("type", JsonPrimitive("TEXT_DETECTION"))
            val features = JsonArray()
            features.add(feature)
            request.add("features", features)

            val requests = JsonArray()
            requests.add(request)

            val imageRequest = JsonObject()
            imageRequest.add("requests", requests)


            // Log a message to indicate that the request is being prepared
            Log.d("FirebaseOCR", "Request prepared: $imageRequest")

            // Continue with your code for making the OCR request...

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

                    // Assuming you have the JSON response in the variable 'finalText'
                    try {
                        val json = JSONObject(finalText)  // Parse the JSON response
                        val responses = json.getJSONArray("responses")  // Get the "responses" array

                        if (responses.length() > 0) {
                            val firstResponse = responses.getJSONObject(0)  // Assuming you want data from the first response

                            val textAnnotations = firstResponse.getJSONArray("textAnnotations")  // Get the "textAnnotations" array

                            // Extract language code from fullTextAnnotation
                            val fullTextAnnotation = firstResponse.getJSONObject("fullTextAnnotation")
                            val languageCode = fullTextAnnotation.optString("locale")
                            Log.d("FirebaseOCR", "Language Code: $languageCode")

                            val extractedDescriptions = mutableListOf<String>() // Create a list to store descriptions

                            for (i in 1 until textAnnotations.length()) {
                                val annotation = textAnnotations.getJSONObject(i)

                                val description = annotation.getString("description")
                                Log.d("FirebaseOCR", "Extracted Text: $description")

                                // Add 'description' to the list
                                extractedDescriptions.add(description)
                            }

                            // Join the descriptions into a single string (or choose how you want to display them)
                            val joinedDescriptions = extractedDescriptions.joinToString(" ")

                            runOnUiThread {
                                recognizedTextView.text = "$joinedDescriptions"


//                                // Call translateText here
//                                translateText("AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c","$joinedDescriptions","en")
                            }
                            val targetLang = "en" // for Arabic
                            val translatedText = translateText("AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c",joinedDescriptions , targetLang)
                            println("Translated Text: $translatedText")
                            runOnUiThread {
                                transTextView.text = "$translatedText"


//                                // Call translateText here
//                                translateText("AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c","$joinedDescriptions","en")
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("FirebaseOCR", "Error parsing JSON: ${e.message}", e)
                        // Handle JSON parsing errors here
                    }
                }
            })

        } catch (e: Exception) {
            // Handle any exceptions that occur during request preparation
            Log.e("FirebaseOCR", "Error preparing OCR request: ${e.message}", e)
            // You may want to show a user-friendly error message here
        }
    }
















    //Tranlation

//    private fun translateWord(word: String): String {
//        val options = TranslatorOptions.Builder()
//            .setSourceLanguage(TranslateLanguage.ENGLISH)
//            .setTargetLanguage(TranslateLanguage.TAMIL)
//            .build()
//        val englishGermanTranslator = Translation.getClient(options)
//
//        var conditions = DownloadConditions.Builder()
//            .requireWifi()
//            .build()
//
//        // Define translatedText outside the inner scope
//        var translatedText = ""
//
//        englishGermanTranslator.downloadModelIfNeeded(conditions)
//            .addOnSuccessListener {
//                // Model downloaded successfully. Okay to start translating.
//                englishGermanTranslator.translate(word)
//                    .addOnSuccessListener { translatedResult ->
//                        // Translation successful.
//                        translatedText = translatedResult.toString()
//                        // Do something with the translatedText if needed
//                    }
//                    .addOnFailureListener { exception ->
//                        // Error.
//                        // Handle the error if needed
//                    }
//                // (Set a flag, unhide the translation UI, etc.)
//            }
//            .addOnFailureListener { exception ->
//                // Model couldn’t be downloaded or other internal error.
//                // Handle the error if needed
//            }
//
//        // Return the translatedText
//        return translatedText
//    }







    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
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