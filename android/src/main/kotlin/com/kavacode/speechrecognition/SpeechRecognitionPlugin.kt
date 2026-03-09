package com.kavacode.speechrecognition

import androidx.annotation.NonNull

import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.content.Intent
import android.os.Bundle;
import android.content.Context

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import java.util.ArrayList
import java.util.Locale

/**
 * SpeechRecognitionPlugin
 */
public class SpeechRecognitionPlugin: FlutterPlugin, MethodCallHandler, RecognitionListener {

    private val LOG_TAG = "SpeechRecognitionPlugin";

    private lateinit var _speech: SpeechRecognizer;
    private lateinit var _channel: MethodChannel;
    private var _transcription = "";
    private lateinit var _recognizerIntent: Intent;
    private lateinit var _context: Context;

    // /**
    //  * Plugin registration.
    //  */
    // companion object {
    //     @JvmStatic
    //     fun registerWith(registrar: Registrar) {
    //         val channel = MethodChannel(registrar.messenger(), "speech_recognition")
    //         channel.setMethodCallHandler(SpeechRecognitionPlugin(registrar.activity(), channel))
    //     }
    // }

    private fun InitSpeech(context: Context) {
        _context = context

        _speech = SpeechRecognizer.createSpeechRecognizer(context)
        _speech.setRecognitionListener(this)

        _recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        _recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        _recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        _recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    //
    // Plugin registration.
    //
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        _channel = MethodChannel(flutterPluginBinding.binaryMessenger, "speech_recognition")
        _channel.setMethodCallHandler(this)
        InitSpeech(flutterPluginBinding.getApplicationContext())
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        _channel.setMethodCallHandler(null)
    }


    //
    // methods router
    // 
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "speech.activate" -> {
                println("- speech activate -")
                val locale: Locale = _context.getResources().getConfiguration().locale
                Log.d(LOG_TAG, "Current Locale : " + locale.toString())
                _channel.invokeMethod("speech.onCurrentLocale", locale.toString())
                result.success(true)
            }
            "speech.listen" -> {
                println("- speech listen -")
                _recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocale(call.arguments.toString()))
                _speech.startListening(_recognizerIntent)
                result.success(true)
            }
            "speech.cancel" -> {
                _speech.cancel()
                result.success(false)
            }
            "speech.stop" -> {
                _speech.stopListening()
                result.success(true)
            }
            "speech.destroy" -> {
                _speech.cancel()
                _speech.destroy()
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    private fun getLocale(code: String) : Locale {
        val localeParts = code.split("_")
        if(localeParts.size > 1){
            return Locale(localeParts[0], localeParts[1])
        }else{
            return Locale("en", "US")
        }
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.d(LOG_TAG, "onReadyForSpeech")
        _channel.invokeMethod("speech.onSpeechAvailability", true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(LOG_TAG, "onRecognitionStarted")
        _transcription = ""
        _channel.invokeMethod("speech.onRecognitionStarted", null)
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d(LOG_TAG, "onRmsChanged : " + rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.d(LOG_TAG, "onBufferReceived")
    }

    override fun onEndOfSpeech() {
        Log.d(LOG_TAG, "onEndOfSpeech")
        _channel.invokeMethod("speech.onRecognitionComplete", _transcription)
    }

    override fun onError(error: Int) {
        Log.d(LOG_TAG, "onError : " + error)
        _channel.invokeMethod("speech.onSpeechAvailability", false)
        _channel.invokeMethod("speech.onError", error)
    }

    override fun onPartialResults(partialResults: Bundle) {
        Log.d(LOG_TAG, "onPartialResults...")
        val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null) {
            _transcription = matches.get(0)
        }
        sendTranscription(false)
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.d(LOG_TAG, "onEvent : " + eventType)
    }

    override fun onResults(results: Bundle) {
        Log.d(LOG_TAG, "onResults...")
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null) {
            _transcription = matches.get(0)
            Log.d(LOG_TAG, "onResults -> " + _transcription)
            sendTranscription(true)
        }
        sendTranscription(false)
    }

    private fun sendTranscription(isFinal: Boolean) {
        _channel.invokeMethod(if(isFinal) "speech.onRecognitionComplete" else "speech.onSpeech", _transcription)
    }
}
