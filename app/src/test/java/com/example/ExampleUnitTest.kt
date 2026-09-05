package com.example

import com.example.audiomidi.api.dsp.YinPitchDetector
import com.example.audiomidi.api.midi.StandardMidiFileWriter
import com.example.audiomidi.api.model.MidiNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ExampleUnitTest {

  @Test
  fun testMidiPitchConversions() {
    // 440 Hz is Concert A4 (MIDI 69)
    val a4Midi = MidiNote.freqToMidi(440.0f)
    assertEquals(69, a4Midi)
    assertEquals("A4", MidiNote.midiToName(69))

    // Middle C is C4 (MIDI 60)
    val c4Midi = MidiNote.freqToMidi(261.63f)
    assertEquals(60, c4Midi)
    assertEquals("C4", MidiNote.midiToName(60))

    // Frequency calculation
    val freqA4 = MidiNote.midiToFreq(69)
    assertEquals(440.0f, freqA4, 0.01f)
  }

  @Test
  fun testStandardMidiFileWriter() {
    val writer = StandardMidiFileWriter(ppq = 480)
    val notes = listOf(
      MidiNote(
        noteNumber = 60,
        pitchName = "C4",
        frequencyHz = 261.63f,
        startTimeMs = 0L,
        durationMs = 500L,
        velocity = 100
      ),
      MidiNote(
        noteNumber = 64,
        pitchName = "E4",
        frequencyHz = 329.63f,
        startTimeMs = 500L,
        durationMs = 500L,
        velocity = 95
      )
    )

    val midiBytes = writer.writeToBytes(notes, bpm = 120, trackName = "Test Track")

    // Check SMF Header signature "MThd"
    assertTrue(midiBytes.size > 22)
    assertEquals('M'.code.toByte(), midiBytes[0])
    assertEquals('T'.code.toByte(), midiBytes[1])
    assertEquals('h'.code.toByte(), midiBytes[2])
    assertEquals('d'.code.toByte(), midiBytes[3])

    // Verify format 0 and 1 track
    assertEquals(0, midiBytes[9].toInt())
    assertEquals(1, midiBytes[11].toInt())

    // Check Track signature "MTrk" at index 14
    assertEquals('M'.code.toByte(), midiBytes[14])
    assertEquals('T'.code.toByte(), midiBytes[15])
    assertEquals('r'.code.toByte(), midiBytes[16])
    assertEquals('k'.code.toByte(), midiBytes[17])
  }

  @Test
  fun testYinPitchDetectionOnPureTone() {
    val sampleRate = 22050
    val durationSamples = 2048
    val targetFreq = 440.0 // A4

    val audio = FloatArray(durationSamples)
    for (i in 0 until durationSamples) {
      val t = i.toDouble() / sampleRate
      audio[i] = sin(2.0 * PI * targetFreq * t).toFloat()
    }

    val yin = YinPitchDetector(sampleRate = sampleRate, bufferSize = 1024, threshold = 0.15f)
    val result = yin.getPitch(audio, 0)

    assertTrue("YIN probability should be high on pure tone", result.probability > 0.8f)
    assertEquals(targetFreq.toFloat(), result.pitchHz, 5.0f)
  }
}
