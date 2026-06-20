package com.invoiceflow.billing.ui.screens.barcode

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Barcode Scanner Screen using CameraX and ML Kit
 */
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var flashEnabled by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        CameraPreview(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onBarcodeDetected = onBarcodeDetected,
            flashEnabled = flashEnabled
        )
        
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, Color.White, MaterialTheme.shapes.medium)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            IconButton(
                onClick = { flashEnabled = !flashEnabled },
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, Color.White, MaterialTheme.shapes.medium)
            ) {
                Icon(
                    imageVector = if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Toggle Flash",
                    tint = Color.White
                )
            }
        }
        
        // Scan Frame Guide
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                .border(
                    2.dp,
                    Color.Green.copy(alpha = 0.7f),
                    MaterialTheme.shapes.medium
                )
        )
        
        // Instructions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position barcode within the frame",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scanning will happen automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onBarcodeDetected: (String) -> Unit,
    flashEnabled: Boolean
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera: Camera? by remember { mutableStateOf(null) }
    
    LaunchedEffect(Unit) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        context.mainExecutor,
                        PreviewView(context).surfaceProvider
                    )
                }
            
            // Image Analysis for barcode scanning
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    val executor = ContextCompat.getMainExecutor(context)
                    analysis.setAnalyzer(executor) { imageProxy ->
                        processBarcodeImage(
                            imageProxy = imageProxy,
                            onBarcodeDetected = onBarcodeDetected
                        )
                    }
                }
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    // Update flash state
    LaunchedEffect(flashEnabled) {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(flashEnabled)
            }
        }
    }
    
    // Actual Camera Preview View
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun processBarcodeImage(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image ?: return
    
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    
    val scanner = BarcodeScanning.getClient()
    
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                // Get first barcode value
                val barcodeValue = barcodes.firstOrNull()?.rawValue
                if (!barcodeValue.isNullOrBlank()) {
                    onBarcodeDetected(barcodeValue)
                }
            }
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

// Extension for main executor
val Context.mainExecutor: ExecutorService
    get() = Executors.newSingleThreadExecutor()
