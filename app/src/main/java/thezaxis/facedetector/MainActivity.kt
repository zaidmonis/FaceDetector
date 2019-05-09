package thezaxis.facedetector

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.*
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.Facing
import thezaxis.facedetector.helpers.FaceBoundsOverlay
import thezaxis.facedetector.helpers.FaceDetector
import thezaxis.facedetector.models.Frame
import thezaxis.facedetector.models.Size

class MainActivity : AppCompatActivity() {
    override fun onStop() {
        super.onStop()
        try{
            System.exit(0)      //so that we can properly destroy cameraView, because explicitly calling the cameraView.destroy() method wasn't working for some reasons(maybe the library is still buggy)
        }catch (e: java.lang.Exception){
            Toast.makeText(this@MainActivity, ""+e, Toast.LENGTH_SHORT)
        }
    }

    /*val ORIENTATIONS = SparseIntArray()

    init{
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }*/

    var cameraView : CameraView?= null
    var facesBoundsOverlay : FaceBoundsOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView : TextView = findViewById(R.id.textView)
        textView.setTextColor(Color.WHITE)
        //val cameraSwitchButton : ImageButton = findViewById(R.id.camera_change_button)
        facesBoundsOverlay = findViewById(R.id.facesbounds_overlay) as FaceBoundsOverlay

        cameraView = findViewById(R.id.camera_view) as CameraView

        cameraView!!.setLifecycleOwner(this)
        cameraView!!.facing = Facing.FRONT
        val faceDetector: FaceDetector by lazy {
            FaceDetector(facesBoundsOverlay!!)
        }
        /*cameraSwitchButton.setOnClickListener {
            if(cameraView.facing == Facing.FRONT){

                cameraView.facing = Facing.BACK
                cameraSwitchButton.background = this.resources.getDrawable(R.drawable.camera_front)
            }
            else{
                cameraView.facing = Facing.FRONT
                cameraSwitchButton.background = this.resources.getDrawable(R.drawable.camera_back)
            }
        }*/
        /*val image = FirebaseVisionImage.fromMediaImage()*/

        startProcessing(textView, faceDetector, cameraView!!);
    }

    private fun startProcessing(textView: TextView, faceDetector: FaceDetector, cameraView: CameraView) {
        cameraView.addFrameProcessor {
            /*faceDetector.process(
                Frame(
                    data = it.data,
                    rotation = it.rotation,
                    size = Size(it.size.width, it.size.height),
                    format = it.format,
                    isCameraFacingBack = false)
            )*/
            var frame = Frame(
                data = it.data,
                rotation = it.rotation,
                size = Size(it.size.width, it.size.height),
                format = it.format,
                isCameraFacingBack = false)
            faceDetector.process(frame)
            findFaces(frame, textView)
        }
    }


    private fun findFaces(frame : Frame, textView : TextView){
         val metaData = FirebaseVisionImageMetadata.Builder()
             .setWidth(frame.size.width)
             .setHeight(frame.size.height)
             .setFormat(frame.format)
             .setRotation(frame.rotation / 90)
             .build()
         val image = FirebaseVisionImage.fromByteArray(frame.data!!, metaData)
         //var faces :List<FirebaseVisionFace>
         val faceDetectorOptions: FirebaseVisionFaceDetectorOptions by lazy {
             FirebaseVisionFaceDetectorOptions.Builder()
                 .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                 .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                 .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                 .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                 .setMinFaceSize(0.15f)
                 .enableTracking()
                 .build()
         }


         val detector = FirebaseVision.getInstance().getVisionFaceDetector(faceDetectorOptions)
         detector.detectInImage(image).addOnSuccessListener { faces->
             //decodeFaces(faces)
             if (faces.size>=1){
                 textView.setText("Faces: "+faces.size)
             }
             else{
                 textView.setText("Show me your face!")
             }
         }.addOnFailureListener(
             object : OnFailureListener {
                 override fun onFailure(e: Exception) {
                     // Task failed with an exception
                     // ...
                     var t = Toast.makeText(this@MainActivity, "Failed: "+e, Toast.LENGTH_SHORT)
                     t.show()
                 }
             })


    }

    /**
     * Here we can decode faces  and their info, but I'm not doing this right now because of the project requirements
     */
    private fun decodeFaces(faces: List<FirebaseVisionFace>) {


        //textView!!.setText("Total detected faces: " +faces.size)
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


    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
*/
}
