package com.ykato.slw

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View

class MainActivity : AppCompatActivity(), View.OnClickListener, SensorEventListener {
    private val TAG = MainActivity::class.java.simpleName
    private var surfaceHolder : SlwSurfaceHolder? = null
    private var sensorManager: SensorManager? = null

    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun setSensor() {
        sensorManager!!.registerListener(
                this,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME)
    }

    private fun unsetSensor() {
        sensorManager!!.unregisterListener(this)
    }

    private fun destroySensor() {
        sensorManager = null
    }

    // AppCompatActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSensor()
    }

    override fun onResume() {
        super.onResume()

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        surfaceView.setOnClickListener(this)
        surfaceHolder = SlwSurfaceHolder(surfaceView)

        setSensor()
    }

    override fun onPause() {
        super.onPause()
        surfaceHolder = null

        unsetSensor()
    }

    override fun onDestroy() {
        super.onDestroy()

        destroySensor()
    }

    // OnClickListener
    override fun onClick(p0: View?) {
        if (surfaceHolder != null) {
            surfaceHolder!!.click()
        }
    }

    // SensorEventListener
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d(TAG, "onAccuracyChanged")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                when {
                    event.values[1] == 0F -> surfaceHolder!!.flat()
                    event.values[1] > 0 -> surfaceHolder!!.lean(true)
                    else -> surfaceHolder!!.lean(false)
                }
            }
        }
    }
}