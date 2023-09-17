package com.example.touraround
import com.example.touraround.R
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.*
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
//import com.google.firebase.ml.vision.FirebaseVision
//import com.google.firebase.ml.vision.common.FirebaseVisionImage
//import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
//import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.ByteArrayOutputStream



@ExperimentalGetImage class Translator: AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var recognizedTextView: TextView
    private lateinit var capturedImageView: ImageView
    private lateinit var functions: FirebaseFunctions
// ...



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
        viewFinder = findViewById(R.id.viewFinder)
        cameraExecutor = Executors.newSingleThreadExecutor()

        recognizedTextView = findViewById(R.id.recognizedTextView)
        capturedImageView = findViewById(R.id.capturedImageView)
        capturedImageView.visibility = View.GONE

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

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val previewView = findViewById<PreviewView>(R.id.viewFinder)
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



    private fun recognizeText(bitmap: Bitmap): String {

        // Scale down bitmap size

        val bitmap = scaleBitmapDown(bitmap, 640)
        // Convert bitmap to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        functions = Firebase.functions

         fun annotateImage(requestJson: String): Task<JsonElement> {
            return functions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith { task ->
                    // This continuation runs on either success or failure, but if the task
                    // has failed then result will throw an Exception which will be
                    // propagated down.
                    val result = task.result?.data
                    JsonParser.parseString(Gson().toJson(result))
                }
        }


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

            // Log a message to indicate that the request is being prepared
            Log.d("FirebaseOCR", "Request prepared: $request")

            // Continue with your code for making the OCR request...
        } catch (e: Exception) {
            // Handle any exceptions that occur during request preparation
            Log.e("FirebaseOCR", "Error preparing OCR request: ${e.message}", e)
            // You may want to show a user-friendly error message here
        }


        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Task failed with an exception

                    // ...
                } else {
                    // Task completed successfully
                    val annotation = task.result!!.asJsonArray[0].asJsonObject["fullTextAnnotation"].asJsonObject
                    System.out.format("%nComplete annotation:")
                    System.out.format("%n%s", annotation["text"].asString)
                    val finaltext =annotation["text"].asString
                    Log.d(ContentValues.TAG, "OCRED TEXT: $finaltext")
                    recognizedTextView.text=finaltext

                    // ...
                }
            }


        val recognizedText="1212"


        return recognizedText

    }









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