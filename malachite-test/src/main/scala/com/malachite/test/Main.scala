package com.malachite.test

import com.malachite.core.MainBase

class Main extends MainBase

object Main {
	def main(args: Array[String]): Unit = {
		// creating an instance of Main class...
		val main = new Main
		// calling the run() method...
		main.run(args, classOf[TestApplication])
	}
}
