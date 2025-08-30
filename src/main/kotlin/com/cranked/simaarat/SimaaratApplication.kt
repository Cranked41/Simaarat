package com.cranked.simaarat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimaaratApplication

fun main(args: Array<String>) {
   // System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)

    runApplication<SimaaratApplication>(*args)
}
