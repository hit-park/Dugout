package com.dugout.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DugoutApplication

fun main(args: Array<String>) {
    runApplication<DugoutApplication>(*args)
}
