package fr.isen.deluc.be_tri

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import fr.isen.deluc.be_tri.databinding.ActivityMainBinding
import fr.isen.deluc.be_tri.ml.TfLiteModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding;
    lateinit var photoPath: String;
    val REQUEST_TAKE_PHOTO = 1;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(binding.root);

        scanFunction();
    }
    private fun scanFunction(){
        binding.scanButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if(intent.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (e:IOException) { }
                if(photoFile != null) {
                    val photoUri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        photoFile
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(intent, 1)
                    true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            binding.image.setImageURI(Uri.parse(photoPath))
            val bitmapImage  = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(photoPath))
            var resized: Bitmap = Bitmap.createScaledBitmap(bitmapImage, 224, 224, true)

            val model = TfLiteModel.newInstance(this)
// Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val tensorBuffer = TensorImage.fromBitmap(resized)
            var byteBuffer = tensorBuffer.buffer
            inputFeature0.loadBuffer(byteBuffer)

// Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

// Releases model resources if no longer used.
            model.close()
        }
    }

    private fun createImageFile(): File? {
        val fileName = "MyPicture"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            fileName,
            ".jpg",
            storageDir,
        )
        photoPath = image.absolutePath

        return image
    }
}