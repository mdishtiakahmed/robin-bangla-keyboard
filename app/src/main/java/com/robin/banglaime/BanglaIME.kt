package com.robin.banglaime

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import java.util.Locale

/**
 * Core IME Service.
 * Developed by Md Ishtiak Ahmed Robin.
 */
class BanglaIME : InputMethodService(), KeyboardView.OnKeyboardActionListener, RecognitionListener {

    private var kv: KeyboardView? = null
    private var keyboardEnglish: Keyboard? = null
    private var keyboardBangla: Keyboard? = null
    private var keyboardSymbols: Keyboard? = null
    
    // Modes: 0=English, 1=Bangla Direct, 2=Avro Phonetic
    private var currentMode = 0 
    private var isCaps = false
    
    // Voice Typing
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // Key codes (Match XML)
    companion object {
        const val CODE_MODE_SWITCH = -101
        const val CODE_VOICE = -102
        const val CODE_EMOJI = -103
        const val CODE_SYMBOLS = -104
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    override fun onCreateInputView(): View {
        kv = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboardEnglish = Keyboard(this, R.xml.qwerty)
        keyboardBangla = Keyboard(this, R.xml.bangla)
        keyboardSymbols = Keyboard(this, R.xml.symbols)
        
        kv?.keyboard = keyboardEnglish
        kv?.setOnKeyboardActionListener(this)
        return kv!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        PhoneticEngine.reset()
        updateKeyboardLayout()
    }

    private fun updateKeyboardLayout() {
        val modeLabel = when(currentMode) {
            0 -> "EN"
            1 -> "BN"
            2 -> "AVRO"
            else -> "EN"
        }
        // Ideally update a key label here to show current mode
        
        when(currentMode) {
            0, 2 -> kv?.keyboard = keyboardEnglish // Avro uses English layout
            1 -> kv?.keyboard = keyboardBangla
        }
    }

    // --- Volume Key Cursor Control ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Move Cursor LEFT
                val ic = currentInputConnection
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Move Cursor RIGHT
                val ic = currentInputConnection
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // Prevent system volume change while controlling cursor
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        
        playClick(primaryCode)

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                PhoneticEngine.reset() // Reset logic on delete
                ic.deleteSurroundingText(1, 0)
            }
            Keyboard.KEYCODE_SHIFT -> {
                isCaps = !isCaps
                kv?.isShifted = isCaps
                kv?.invalidateAllKeys()
            }
            Keyboard.KEYCODE_DONE -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            CODE_MODE_SWITCH -> {
                // Cycle: Eng -> Bangla -> Avro -> Eng
                currentMode = (currentMode + 1) % 3
                val toastMsg = when(currentMode) {
                    0 -> getString(R.string.mode_english)
                    1 -> getString(R.string.mode_bangla)
                    2 -> getString(R.string.mode_avro)
                    else -> ""
                }
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                updateKeyboardLayout()
            }
            CODE_VOICE -> startVoiceInput()
            CODE_SYMBOLS -> {
                 // Toggle logic for symbols
                 if (kv?.keyboard == keyboardSymbols) {
                     updateKeyboardLayout()
                 } else {
                     kv?.keyboard = keyboardSymbols
                 }
            }
            else -> {
                if (currentMode == 2) {
                    // Avro Mode
                    val char = primaryCode.toChar()
                    if (Character.isLetter(char)) {
                         // Simple logic: If uppercase, assume shift was pressed
                         val input = if (isCaps) char.uppercaseChar() else char.lowercaseChar()
                         
                         // Process via engine
                         val (deleteCount, result) = PhoneticEngine.process(input)
                         
                         if (deleteCount > 0) {
                             ic.deleteSurroundingText(deleteCount, 0)
                         }
                         ic.commitText(result, 1)
                    } else {
                        // Numbers/Punctuation in Avro mode
                        ic.commitText(primaryCode.toChar().toString(), 1)
                    }
                } else {
                    // Standard Mode (Eng or Bangla Direct)
                    var code = primaryCode.toChar()
                    if (Character.isLetter(code) && isCaps) {
                        code = code.uppercaseChar()
                    }
                    ic.commitText(code.toString(), 1)
                }
            }
        }
    }
    
    // --- Voice Typing Implementation ---
    
    private fun startVoiceInput() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "Voice recognition not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if(currentMode == 1) "bn-BD" else "en-US")
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            
            speechRecognizer?.startListening(intent)
            isListening = true
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            currentInputConnection?.commitText("$text ", 1)
        }
        isListening = false
    }

    override fun onError(error: Int) {
        isListening = false
        // specific error handling omitted for brevity
    }
    
    // Boilerplate overrides
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { isListening = false }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- UX Feedback ---
    
    private fun playClick(keyCode: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // In a real app, check Settings preference
        am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(20)
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {
        currentInputConnection?.commitText(text, 1)
    }
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}