package com.ykato.slw

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.*
import android.util.Log

class SlwSurfaceHolder : SurfaceHolder.Callback, Runnable {
    private val holder: SurfaceHolder
    private val surface: SurfaceView
    private val character = Character()

    private var thread: Thread? = null
    private var isCancel = false
    private var isJumping = false
    private var isInTheAir = false
    private var initialVelocity = 0
    private var migrationLength : Int = 0

    enum class Direction {
        NONE, RIGHT, LEFT, TOP, UNDER
    }

    constructor(surface: SurfaceView) {
        holder = surface.holder
        holder.addCallback(this)
        this.surface = surface
    }

    fun jump() {
        if(!isInTheAir) {
            initialVelocity = 70
            isJumping = true
        }
    }

    fun run(isRightDirection: Boolean) {
        if(isRightDirection) {
            character.turnRight()
            migrationLength = 50
        } else {
            character.turnLeft()
            migrationLength = -50
        }
    }

    fun stop() {
        migrationLength = 0
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.argb(0xff, 0x13, 0x4a, 0xac))
    }

    private fun drawGround(canvas: Canvas) : Int {
        val ground = NormalGround()
        val bitmap = ground.createGround(1)
        var x = 0
        val y = (surface.height - bitmap.height).toDouble()

        val paint = Paint()
        while (surface.width > x) {
            canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), paint)
            x += bitmap.width
        }

        return bitmap.height
    }

    private fun drawBlock(canvas: Canvas, destinationX: Double, destinationY: Double): BitmapData {
        val block = NormalBlock()
        val paint = Paint()
        val bitmap = block.createBlock(0)
        canvas.drawBitmap(bitmap, (bitmap.height * 5).toFloat(), (surface.height - bitmap.height * 4).toFloat(), paint)
//        canvas.drawBitmap(bitmap, (bitmap.height * 0).toFloat(), (surface.height - bitmap.height * 4).toFloat(), paint)
        return BitmapData(bitmap, (bitmap.height * 5).toDouble(), (surface.height - bitmap.height * 4).toDouble())
    }

    private fun isHit(bitmap1: BitmapData, bitmap2: BitmapData, bitmap3: BitmapData): Direction {
        if(Math.abs(bitmap1.x - bitmap2.x) < bitmap1.bitmap.width/2 + bitmap2.bitmap.width/2 //横の判定
                    &&
                            Math.abs(bitmap1.y - bitmap2.y) < bitmap1.bitmap.height/2 + bitmap2.bitmap.height/2 //縦の判定
        ) {
            if (b((bitmap1.x + bitmap1.bitmap.width), bitmap1.y,
                            (bitmap1.x + bitmap1.bitmap.width), (bitmap1.y + bitmap1.bitmap.height),
                            bitmap3, bitmap2)) {
                return Direction.RIGHT
            } else if (b(bitmap1.x, (bitmap1.y + bitmap1.bitmap.height),
                            (bitmap1.x + bitmap1.bitmap.width), (bitmap1.y + bitmap1.bitmap.height),
                            bitmap2, bitmap3)) {
                return Direction.UNDER
            } else if (b(bitmap1.x, bitmap1.y,
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

    private fun b(ax: Double, ay: Double, bx: Double, by: Double, bitmap2: BitmapData, bitmap3: BitmapData): Boolean {
        if (a(ax, ay, bx, by, bitmap2.x, bitmap2.y, bitmap3.x, bitmap3.y)) {
            return true
        } else if (a(ax, ay, bx, by, (bitmap2.x + bitmap2.bitmap.width), bitmap2.y,
                        (bitmap3.x + bitmap3.bitmap.width), bitmap3.y)) {
            return true
        } else if (a(ax, ay, bx, by, bitmap2.x, (bitmap2.y + bitmap2.bitmap.height),
                        bitmap3.x, (bitmap3.y + bitmap3.bitmap.height))) {
            return true
        } else {
            return a(ax, ay, bx, by, (bitmap2.x + bitmap2.bitmap.width),
                    (bitmap2.y + bitmap2.bitmap.height),
                    (bitmap3.x + bitmap3.bitmap.width), (bitmap3.y + bitmap3.bitmap.height))
        }
    }
        private fun a(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Boolean {
        var ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
        var tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
        var tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
        var td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);

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
        var time = 1.0
        var x = 0
        var y = 0.0
        while (!isCancel) {
            val canvas = holder.lockCanvas()

            drawBackground(canvas)

            var destinationX = when {
                    surface.width < (x + migrationLength ) -> -character.getWidth()
                    -character.getWidth()  > (x + migrationLength ) -> surface.width
                    else -> x + migrationLength
            }

            val groundHeight = drawGround(canvas)
            var destinationY: Double
            if (isJumping) {
                var hight = calculateHeight(time)
                if (hight <= 0) {
                    time = 1.0
                    isJumping = false
                } else {
                    time++
                }
                destinationY = (surface.height - groundHeight - character.getHeight()).toDouble() - hight
            } else {
                if ((y != 0.0) &&
                        (y != (surface.height - groundHeight - character.getHeight()).toDouble())) {
                    destinationY = y
                } else {
                    destinationY = (surface.height - groundHeight - character.getHeight()).toDouble()
                }
            }


            val characterBitmap =
                    when {
                        isJumping -> character.jump()
                        migrationLength == 0 -> character.stop()
                        else -> character.run()
                    }

            var blockBitmap = drawBlock(canvas, destinationX.toDouble(), destinationY)

            val paint = Paint()

            val direction = isHit(blockBitmap,
                    BitmapData(characterBitmap, destinationX.toDouble(), destinationY),
                    BitmapData(characterBitmap, x.toDouble(), y))
            if (direction == Direction.UNDER) {
                Log.d("hogehoge", "Under")
                canvas.drawBitmap(characterBitmap, destinationX.toFloat(), (blockBitmap.y.toFloat() + blockBitmap.bitmap.height), paint)
                initialVelocity = 0
                x = destinationX
                y = (blockBitmap.y + blockBitmap.bitmap.height)
            } else if (direction == Direction.RIGHT) {
                Log.d("hogehoge", "Right")
                canvas.drawBitmap(characterBitmap, (blockBitmap.x.toFloat() + blockBitmap.bitmap.width),
                        destinationY.toFloat(), paint)
                initialVelocity = 0
                x = (blockBitmap.x + blockBitmap.bitmap.width).toInt()
                y = destinationY
            } else if (direction == Direction.LEFT) {
                Log.d("hogehoge", "Left")
                canvas.drawBitmap(characterBitmap, (blockBitmap.x.toFloat() - character.getWidth()),
                        destinationY.toFloat(), paint)
                initialVelocity = 0
                x = blockBitmap.x.toInt() - character.getWidth()
                y = destinationY
            } else if (direction == Direction.TOP) {
                Log.d("hogehoge", "Top")
                canvas.drawBitmap(characterBitmap, destinationX.toFloat(), (blockBitmap.y - character.getHeight()).toFloat(), paint)
                initialVelocity = 0

                x = destinationX
                y = blockBitmap.y - character.getHeight()
            } else {
                canvas.drawBitmap(characterBitmap, destinationX.toFloat(), destinationY.toFloat(), paint)
                x = destinationX
                y = destinationY
            }

            holder.unlockCanvasAndPost(canvas)
            Thread.sleep(100L)
        }
    }
}