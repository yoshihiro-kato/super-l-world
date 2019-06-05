package com.ykato.slw

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.*
import android.util.Log

class SlwSurfaceHolder : SurfaceHolder.Callback, Runnable {
    private val TAG = SlwSurfaceHolder::class.java.simpleName

    private val surfaceHolder: SurfaceHolder
    private val surfaceView: SurfaceView
    private val character = Character()

    private var thread: Thread? = null
    private var isCancel = false
    private var isJumping = false
    private var isInTheAir = false
    private var initialVelocity = 0
    private var migrationLength = 0

    enum class Direction {
        NONE, RIGHT, LEFT, TOP, UNDER
    }

    constructor(surface: SurfaceView) {
        surfaceHolder = surface.holder
        surfaceHolder.addCallback(this)
        surfaceView = surface
    }

    fun click() {
        if(!isInTheAir) {
            initialVelocity = 70
            isJumping = true
        }
    }

    fun lean(isRight: Boolean) {
        if(isRight) {
            character.turnRight()
            migrationLength = 40
        } else {
            character.turnLeft()
            migrationLength = -40
        }
    }

    fun flat() {
        migrationLength = 0
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.argb(0xff, 0x13, 0x4a, 0xac))
    }

    private fun drawGround(canvas: Canvas) : Int {
        val ground = NormalGround()
        val bitmap = ground.createGround(1)
        var x = 0
        val y = (surfaceView.height - bitmap.height).toDouble()

        val paint = Paint()
        while (surfaceView.width > x) {
            canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), paint)
            x += bitmap.width
        }

        return bitmap.height
    }

    private fun drawBlock(canvas: Canvas): BitmapData {
        val block = NormalBlock()
        val paint = Paint()
        val bitmap = block.createBlock(0)
        canvas.drawBitmap(bitmap, (bitmap.height * 5).toFloat(), (surfaceView.height - bitmap.height * 4).toFloat(), paint)
        return BitmapData(bitmap, (bitmap.height * 5).toDouble(), (surfaceView.height - bitmap.height * 4).toDouble())
    }

    private fun isHit(bitmap1: BitmapData, bitmap2: BitmapData, bitmap3: BitmapData): Direction {
        if(Math.abs(bitmap1.x - bitmap2.x) < bitmap1.bitmap.width/2 + bitmap2.bitmap.width/2 //横の判定
                    &&
                            Math.abs(bitmap1.y - bitmap2.y) < bitmap1.bitmap.height/2 + bitmap2.bitmap.height/2 //縦の判定
        ) {
            if (isIntersect((bitmap1.x + bitmap1.bitmap.width), bitmap1.y,
                            (bitmap1.x + bitmap1.bitmap.width), (bitmap1.y + bitmap1.bitmap.height),
                            bitmap3, bitmap2)) {
                return Direction.RIGHT
            } else if (isIntersect(bitmap1.x, (bitmap1.y + bitmap1.bitmap.height),
                            (bitmap1.x + bitmap1.bitmap.width), (bitmap1.y + bitmap1.bitmap.height),
                            bitmap2, bitmap3)) {
                return Direction.UNDER
            } else if (isIntersect(bitmap1.x, bitmap1.y,
                            bitmap1.x, (bitmap1.y + bitmap1.bitmap.height),
                            bitmap2, bitmap3)) {
                return Direction.LEFT
            } else {
                return Direction.TOP
            }
        } else {
            return Direction.NONE
        }
    }

    private fun isIntersect(ax: Double, ay: Double, bx: Double, by: Double, bitmap2: BitmapData, bitmap3: BitmapData): Boolean {
        return when {
            isIntersect(ax, ay, bx, by, bitmap2.x, bitmap2.y, bitmap3.x, bitmap3.y) -> true
            isIntersect(ax, ay, bx, by, (bitmap2.x + bitmap2.bitmap.width), bitmap2.y,
                    (bitmap3.x + bitmap3.bitmap.width), bitmap3.y) -> true
            isIntersect(ax, ay, bx, by, bitmap2.x, (bitmap2.y + bitmap2.bitmap.height),
                    bitmap3.x, (bitmap3.y + bitmap3.bitmap.height)) -> true
            else -> isIntersect(ax, ay, bx, by, (bitmap2.x + bitmap2.bitmap.width),
                    (bitmap2.y + bitmap2.bitmap.height),
                    (bitmap3.x + bitmap3.bitmap.width), (bitmap3.y + bitmap3.bitmap.height))
        }
    }

    private fun isIntersect(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Boolean {
        var ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax)
        var tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx)
        var tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx)
        var td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx)

        return tc * td <= 0 && ta * tb <= 0
    }

    private fun calculateHeight(time: Double): Double {
        val gravitationalAcceleration = 9.81

        val y = initialVelocity * time + 0.5 * -gravitationalAcceleration * Math.pow(time.toDouble(), 2.toDouble())

        if (y > 0) {
            isInTheAir = true
            return y
        } else {
            isInTheAir = false
            return 0.0
        }
    }

    // SurfaceHolder.Callback
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isCancel = true
        thread = null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        thread = Thread(this)
        thread?.start()
    }

    // Runnable
    override fun run() {
        var time = 2.0
        var x = 0
        var y = 0.0
        while (!isCancel) {
            val canvas = surfaceHolder.lockCanvas()

            drawBackground(canvas)

            var destinationX = when {
                surfaceView.width < (x + migrationLength ) -> -character.getWidth()
                    -character.getWidth()  > (x + migrationLength ) -> surfaceView.width
                    else -> x + migrationLength
            }

            val groundHeight = drawGround(canvas)
            var destinationY: Double
            if (isJumping) {
                var hight = calculateHeight(time)
                if (hight <= 0) {
                    time = 2.0
                    isJumping = false
                } else {
                    time++
                }
                destinationY = (surfaceView.height - groundHeight - character.getHeight()).toDouble() - hight
            } else {
                if ((y != 0.0) &&
                        (y != (surfaceView.height - groundHeight - character.getHeight()).toDouble())) {
                    destinationY = y
                } else {
                    destinationY = (surfaceView.height - groundHeight - character.getHeight()).toDouble()
                }
            }


            val characterBitmap =
                    when {
                        isJumping -> character.jump()
                        migrationLength == 0 -> character.stop()
                        else -> character.run()
                    }

            var blockBitmap = drawBlock(canvas)

            val paint = Paint()

            val direction = isHit(blockBitmap,
                    BitmapData(characterBitmap, destinationX.toDouble(), destinationY),
                    BitmapData(characterBitmap, x.toDouble(), y))
            if (direction == Direction.UNDER) {
                Log.d(TAG, "Under")
                initialVelocity = 0
                x = destinationX
                y = (blockBitmap.y + blockBitmap.bitmap.height)
            } else if (direction == Direction.RIGHT) {
                Log.d(TAG, "Right")
                initialVelocity = 0
                x = (blockBitmap.x + blockBitmap.bitmap.width).toInt()
                y = destinationY
            } else if (direction == Direction.LEFT) {
                Log.d(TAG, "Left")
                initialVelocity = 0
                x = blockBitmap.x.toInt() - character.getWidth()
                y = destinationY
            } else if (direction == Direction.TOP) {
                Log.d(TAG, "Top")
                initialVelocity = 0
                x = destinationX
                y = blockBitmap.y - character.getHeight()
            } else {
                x = destinationX
                y = destinationY
            }
            canvas.drawBitmap(characterBitmap, x.toFloat(), y.toFloat(), paint)

            surfaceHolder.unlockCanvasAndPost(canvas)
            Thread.sleep(100L)
        }
    }
}