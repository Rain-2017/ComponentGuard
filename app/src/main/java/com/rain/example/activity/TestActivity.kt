package com.rain.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rain.example.IMyAidl
import com.rain.example.R


class TestActivity : AppCompatActivity() {

    val list = mutableListOf<IMyAidl>()

    companion object {
        private const val SS = "abc"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Toast.makeText(this, TestActivity.SS, Toast.LENGTH_SHORT).show()
    }
}