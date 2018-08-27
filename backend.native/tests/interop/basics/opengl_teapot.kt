/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import platform.GLUT.*
import platform.OpenGL.*
import platform.OpenGLCommon.*

// Ported from http://openglsamples.sourceforge.net/projects/index.php/blog/index/

private var rotation: GLfloat = 0.0f
private val rotationSpeed: GLfloat = 0.2f

private val windowWidth = 640
private val windowHeight = 480

fun display() {
    // Clear Screen and Depth Buffer
    glClear((GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT).convert())
    glLoadIdentity()

    // Define a viewing transformation
    gluLookAt(4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

    // Push and pop the current matrix stack.
    // This causes that translations and rotations on this matrix wont influence others.

    glPushMatrix()
    glColor3f(1.0f, 0.0f, 0.0f)
    glTranslatef(0.0f, 0.0f, 0.0f)
    glRotatef(rotation, 0.0f, 1.0f, 0.0f)
    glRotatef(90.0f, 0.0f, 1.0f, 0.0f)

    // Draw the teapot
    glutSolidTeapot(1.0)
    glPopMatrix()


    rotation += rotationSpeed
    glutSwapBuffers()
}


fun initialize() {
    // select projection matrix
    glMatrixMode(GL_PROJECTION.convert())

    // set the viewport
    glViewport(0, 0, windowWidth, windowHeight)

    // set matrix mode
    glMatrixMode(GL_PROJECTION.convert())

    // reset projection matrix
    glLoadIdentity()
    val aspect = windowWidth.toDouble() / windowHeight

    // set up a perspective projection matrix
    gluPerspective(45.0, aspect, 1.0, 500.0)

    // specify which matrix is the current matrix
    glMatrixMode(GL_MODELVIEW.convert())
    glShadeModel(GL_SMOOTH.convert())

    // specify the clear value for the depth buffer
    glClearDepth(1.0)
    glEnable(GL_DEPTH_TEST.convert())
    glDepthFunc(GL_LEQUAL.convert())

    // specify implementation-specific hints
    glHint(GL_PERSPECTIVE_CORRECTION_HINT.convert(), GL_NICEST.convert())

    glLightModelfv(GL_LIGHT_MODEL_AMBIENT.convert(), cValuesOf(0.1f, 0.1f, 0.1f, 1.0f))
    glLightfv(GL_LIGHT0.convert(), GL_DIFFUSE.convert(), cValuesOf(0.6f, 0.6f, 0.6f, 1.0f))
    glLightfv(GL_LIGHT0.convert(), GL_SPECULAR.convert(), cValuesOf(0.7f, 0.7f, 0.3f, 1.0f))

    glEnable(GL_LIGHT0.convert())
    glEnable(GL_COLOR_MATERIAL.convert())
    glShadeModel(GL_SMOOTH.convert())
    glLightModeli(GL_LIGHT_MODEL_TWO_SIDE.convert(), GL_FALSE)
    glDepthFunc(GL_LEQUAL.convert())
    glEnable(GL_DEPTH_TEST.convert())
    glEnable(GL_LIGHTING.convert())
    glEnable(GL_LIGHT0.convert())
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
}

fun main(args: Array<String>) {
    // initialize and run program
    memScoped {
        val argc = alloc<IntVar>().apply { value = 0 }
        glutInit(argc.ptr, null) // TODO: pass real args
    }

    // Display Mode
    glutInitDisplayMode((GLUT_RGB or GLUT_DOUBLE or GLUT_DEPTH).convert())

    // Set window size
    glutInitWindowSize(windowWidth, windowHeight)

    // create Window
    glutCreateWindow("The GLUT Teapot")

    // register Display Function
    glutDisplayFunc(staticCFunction(::display))

    // register Idle Function
    glutIdleFunc(staticCFunction(::display))

    initialize()

    // run GLUT mainloop
    glutMainLoop()
}