/*
	Copyright https://github.com/zerokol/JoystickView
	JoystickView by AJ Alves is licensed under a Creative Commons Attribution-ShareAlike 3.0 Unported License.
	Based on a work at github.com and page zerokol.com/2012/03/joystickview-uma-view-customizada-que.html.
	Permissions beyond the scope of this license may be available at http://github.com/zerokol.
 */

package com.yangpeiwen.remotefish;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View implements Runnable {

	public final static long DEFAULT_LOOP_INTERVAL = 40; // 40 ms
	public final static int FRONT = 3;
	public final static int FRONT_RIGHT = 2;
	public final static int RIGHT = 1;
	public final static int RIGHT_BOTTOM = 8;
	public final static int BOTTOM = 7;
	public final static int BOTTOM_LEFT = 6;
	public final static int LEFT = 5;
	public final static int LEFT_FRONT = 4;

	// Variables
	private OnJoystickMoveListener onJoystickMoveListener; // Listener
	private Thread thread = new Thread(this);
	private long loopInterval = DEFAULT_LOOP_INTERVAL;
	private int xPosition = 0; // Touch x position
	private int yPosition = 0; // Touch y position
	private double centerX = 0; // Center view x position
	private double centerY = 0; // Center view y position
	private int joystickRadius;
	private int buttonRadius;
	private int lastAngle = 0;
	private int action = MotionEvent.ACTION_UP;

	public JoystickView(Context context) {
		super(context);
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
//		initJoystickView();
	}

	public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
//		initJoystickView();
	}

	@Override
	protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
		super.onSizeChanged(xNew, yNew, xOld, yOld);
		// before measure, get the center of view
		xPosition = getWidth() / 2;
		yPosition = getWidth() / 2;
		int d = Math.min(xNew, yNew);
		buttonRadius = (int) (d / 2 * 0.25);
		joystickRadius = (int) (d / 2 * 0.75);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// setting the measured values to resize the view to a certain width and
		// height
		int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

		setMeasuredDimension(d, d);

	}

	private int measure(int measureSpec) {
		int result;

		// Decode the measurement specifications.
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.UNSPECIFIED) {
			// Return a default size of 200 if no bounds are specified.
			result = 200;
		} else {
			// As you want to fill the available space
			// always return the full available bounds.
			result = specSize;
		}
		return result;
	}

	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	Paint button = new Paint(Paint.ANTI_ALIAS_FLAG);

	int alpha = 0x70;

	@Override
	protected void onDraw(Canvas canvas) {
		if (action == MotionEvent.ACTION_DOWN | action == MotionEvent.ACTION_MOVE) {
			alpha = 0x70;
		} else if (action == MotionEvent.ACTION_UP) {
			alpha = 0x30;
		}

		paint.setColor(0x30000000);
		paint.setStyle(Paint.Style.FILL);

		button.setColor(alpha<<24);
		button.setStyle(Paint.Style.FILL);

		// super.onDraw(canvas);
		centerX = (getWidth()) / 2;
		centerY = (getHeight()) / 2;

		// painting the main circle
		canvas.drawCircle((int) centerX, (int) centerY, joystickRadius, paint);

		// painting the move button
		canvas.drawCircle(xPosition, yPosition, buttonRadius, button);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		action = event.getAction();
		xPosition = (int) event.getX();
		yPosition = (int) event.getY();
		double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
				+ (yPosition - centerY) * (yPosition - centerY));
		if (abs > joystickRadius - buttonRadius) {
			xPosition = (int) ((xPosition - centerX) * (joystickRadius-buttonRadius) / abs + centerX);
			yPosition = (int) ((yPosition - centerY) * (joystickRadius-buttonRadius) / abs + centerY);
		}
		invalidate();
		if (event.getAction() == MotionEvent.ACTION_UP) {
			xPosition = (int) centerX;
			yPosition = (int) centerY;
			thread.interrupt();
			if (onJoystickMoveListener != null)
				onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
						getDirection());
		}
		if (onJoystickMoveListener != null
				&& event.getAction() == MotionEvent.ACTION_DOWN) {
			if (thread != null && thread.isAlive()) {
				thread.interrupt();
			}
			thread = new Thread(this);
			thread.start();
			if (onJoystickMoveListener != null)
				onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
						getDirection());
		}
		return true;
	}

	private int getAngle() {
		double RAD = 57.2957795;
		if (xPosition > centerX) {
			if (yPosition < centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY)
						/ (xPosition - centerX))
						* RAD + 90);
			} else if (yPosition > centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY)
						/ (xPosition - centerX)) * RAD) + 90;
			} else {
				return lastAngle = 90;
			}
		} else if (xPosition < centerX) {
			if (yPosition < centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY)
						/ (xPosition - centerX))
						* RAD + 270);
			} else if (yPosition > centerY) {
				return lastAngle = (int) (Math.atan((yPosition - centerY)
						/ (xPosition - centerX)) * RAD) + 270;
			} else {
				return lastAngle = 90;
			}
		} else {
			if (yPosition <= centerY) {
				return lastAngle = 0;
			} else {
				if (lastAngle < 0) {
					return lastAngle = 180;
				} else {
					return lastAngle = 180;
				}
			}
		}
	}

	private int getPower() {
		double power = (100 * Math.sqrt((xPosition - centerX)
				* (xPosition - centerX) + (yPosition - centerY)
				* (yPosition - centerY)) / (joystickRadius-buttonRadius));
		if(power>98)power=100;
		return (int) power;
	}

	private int getDirection() {
		if (lastAngle == 0) {
			if(yPosition - centerY == 0)return 0;
			else return 3;
		}
		int a = 0;
		if (lastAngle <= 0) {
			a = (lastAngle * -1) + 90;
		} else if (lastAngle > 0) {
			if (lastAngle <= 90) {
				a = 90 - lastAngle;
			} else {
				a = 360 - (lastAngle - 90);
			}
		}

		int direction = ((a + 22) / 45) + 1;

		if (direction > 8) {
			direction = 1;
		}
		return direction;
	}

	public void setOnJoystickMoveListener(OnJoystickMoveListener listener,
			long repeatInterval) {
		this.onJoystickMoveListener = listener;
		this.loopInterval = repeatInterval;
	}



	public interface OnJoystickMoveListener {
		void onValueChanged(int angle, int power, int direction);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			post(new Runnable() {
				public void run() {
					if (onJoystickMoveListener != null)
						onJoystickMoveListener.onValueChanged(getAngle(),
								getPower(), getDirection());
				}
			});

			SystemClock.sleep(loopInterval);
		}
	}
}
