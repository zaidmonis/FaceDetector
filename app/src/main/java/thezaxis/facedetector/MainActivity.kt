package thezaxis.facedetector

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.*
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.Facing
import husaynhakeem.io.facedetector.FaceBoundsOverlay
import husaynhakeem.io.facedetector.FaceDetector
import husaynhakeem.io.facedetector.models.Frame
import husaynhakeem.io.facedetector.models.Size
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {


    val ORIENTATIONS = SparseIntArray()

    init{
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView : TextView = findViewById(R.id.textView)



        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .enableTracking()
            .build()

        // Real-time contour detection of multiple faces
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()


        var firebaseFaceDetectorWrapper : FirebaseVisionFaceDetector
        //firebaseFaceDetectorWrapper.detectInImage()










        var facesBoundsOverlay = findViewById(R.id.facesbounds_overlay) as FaceBoundsOverlay
        var cameraView = findViewById(R.id.camera_view) as CameraView

        cameraView.setLifecycleOwner(this)
        cameraView.facing = Facing.FRONT

        cameraView.addFrameProcessor {
            var image : FirebaseVisionImage
            var bmp : Bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.icon)
            var frame : Frame = Frame(
                data = it.data,
                rotation = it.rotation,
                size = Size(it.size.width, it.size.height),
                format = it.format,
                isCameraFacingBack = false)
            if (frame.data?.size!! != null){
                //bmp = BitmapFactory.decodeByteArray(frame.data, 0, frame.data?.size!!)
                image = FirebaseVisionImage.fromBitmap(bmp)
            }
            //var bmp: Bitmap = BitmapFactory.decodeByteArray(frame.data, 0, frame.data?.size!!)

            image = FirebaseVisionImage.fromBitmap(bmp)



            val metadata = FirebaseVisionImageMetadata.Builder()
                .setWidth(480) // 480x360 is typically sufficient for
                .setHeight(360) // image recognition
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .build()
            var bfr : ByteBuffer = ByteBuffer.wrap(frame.data)
            val image2 = FirebaseVisionImage.fromByteArray(frame.data!!, extractFrameMetadata(frame))

            val detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts)


            detector.detectInImage(image2)
                .addOnSuccessListener { faces ->
                    // Task completed successfully
                    // ...
                    if (faces.size >=1){
                        /*var t = Toast.makeText(this@MainActivity, ""+faces[0].smilingProbability, Toast.LENGTH_SHORT)
                        t.show()*/
                        decodeFaces(faces, textView)
                    }

                    //decodeFaces(faces)
                }
                .addOnFailureListener(
                    object : OnFailureListener {
                        override fun onFailure(e: Exception) {
                            // Task failed with an exception
                            // ...
                            var t = Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT)
                            t.show()
                        }
                    })




        }
        /*cameraView.setLifecycleOwner(this)
        cameraView.facing = Facing.FRONT*/
        /*val faceDetector: FaceDetector by lazy {
            FaceDetector(facesBoundsOverlay)
        }
        val image = FirebaseVisionImage.fromMediaImage()

        cameraView.addFrameProcessor {
            faceDetector.process(
                Frame(
                    data = it.data,
                    rotation = it.rotation,
                    size = Size(it.size.width, it.size.height),
                    format = it.format,
                    isCameraFacingBack = false)
            )
        }
        cameraView.setLifecycleOwner(this)
        cameraView.facing= Facing.FRONT*/


        //val image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation)

    }
        /**
         * Get the angle by which an image must be rotated given the device's current
         * orientation.
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Throws(CameraAccessException::class)
        fun getRotationCompensation(cameraId: String, activity: Activity, context: Context): Int {
            // Get the device's current rotation relative to its "native" orientation.
            // Then, from the ORIENTATIONS table, look up the angle the image must be
            // rotated to compensate for the device's rotation.
            val deviceRotation = activity.windowManager.defaultDisplay.rotation
            var rotationCompensation = ORIENTATIONS.get(deviceRotation)

            // On most devices, the sensor orientation is 90 degrees, but for some
            // devices it is 270 degrees. For devices with a sensor orientation of
            // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
            val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
            val sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

            // Return the corresponding FirebaseVisionImageMetadata rotation value.
            val result: Int
            when (rotationCompensation) {
                0 -> result = FirebaseVisionImageMetadata.ROTATION_0
                90 -> result = FirebaseVisionImageMetadata.ROTATION_90
                180 -> result = FirebaseVisionImageMetadata.ROTATION_180
                270 -> result = FirebaseVisionImageMetadata.ROTATION_270
                else -> {
                    result = FirebaseVisionImageMetadata.ROTATION_0
                    Log.e("\n\n\nblablabla\n\n\n", "Bad rotation value: $rotationCompensation")
                }
            }
            return result
        }
    /*private val faceDetector: FaceDetector by lazy {
        FaceDetector(facesBoundsOverlay)
    }*/




    private fun extractFrameMetadata(frame: Frame): FirebaseVisionImageMetadata {
        return FirebaseVisionImageMetadata.Builder()
            .setWidth(frame.size.width)
            .setHeight(frame.size.height)
            .setFormat(frame.format)
            .setRotation(frame.rotation / 90)
            .build()

    }







    private fun decodeFaces(faces : List<FirebaseVisionFace>, textView : TextView){
        for (face in faces) {
            val bounds = face.boundingBox
            val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
            val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
            leftEar?.let {
                val leftEarPos = leftEar.position
            }

            // If contour detection was enabled:
            val leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).points
            val upperLipBottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).points

            // If classification was enabled:
            if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                val smileProb = face.smilingProbability
                /*var t = Toast.makeText(this@MainActivity, ""+smileProb, Toast.LENGTH_SHORT)
                t.show()*/
                textView.setText("" +smileProb)
            }
            if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                val rightEyeOpenProb = face.rightEyeOpenProbability
            }

            // If face tracking was enabled:
            if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                val id = face.trackingId
            }
        }
    }
}
