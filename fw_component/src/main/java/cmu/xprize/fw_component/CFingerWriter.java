//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.fw_component;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cmu.xprize.ltk.LipiTKJNIInterface;
import cmu.xprize.ltk.RecResult;
import cmu.xprize.ltk.Stroke;
import cmu.xprize.util.TCONST;


public class CFingerWriter extends View implements OnTouchListener {

    private Context            mContext;
    private ITextSink          mLinkedView;
    private int                mLinkedViewID = -1;
    protected boolean          _enabled = false;

    private Path               mPath;
    private Paint              mPaint;
    private Paint              mPaintBase;
    private Paint              mPaintUpper;
    private float              mX, mY;

    private static final float TOLERANCE = 5;

    private LipiTKJNIInterface _recognizer;
    private String             _configFolder;
    private boolean            _initialized   = false;
    private RecognizerThread   _recThread;
    private boolean            _isRecognizing = false;
    private Stroke[]           _recStrokes;
    private RecResult[]        _recResults;
    protected ArrayList<String>_recChars;
    private String             _constraint;

    private Stroke             _currentStroke;
    private ArrayList<Stroke>  _currentStrokeStore;
    private int[]              _screenCoord  = new int[2];
    private Boolean            _watchable    = false;

    private Boolean            _touchStarted = false;
    private String             _onStartWriting;
    private String             _onRecognition;

    private RecogDelay            _counter;
    private long                  _time;
    private long                  _prevTime;

    private LocalBroadcastManager bManager;
    public static String          RECMSG = "CHAR_RECOG";

    private static int            RECDELAY   = 400;              // Just want the end timeout
    private static int            RECDELAYNT = RECDELAY+500;     // inhibit ticks

    // This is used to map "recognizer types" to LTK project ids
    //
    static public HashMap<String, String> recogMap = new HashMap<String, String>();

    static {
        recogMap.put("EN_STD_ALPHA", "SHAPEREC_ALPHANUM");
        recogMap.put("EN_STD_NUM", "SHAPEREC_NUMERALS");
    }

    // This is used to map "recognizer types" to LTK project folders
    //
    static public HashMap<String, String> folderMap = new HashMap<String, String>();

    static {
        folderMap.put("EN_STD_ALPHA", "/projects/alphanumeric/config/");
        folderMap.put("EN_STD_NUM", "/projects/demonumerals/config/");
    }


    private static final String   TAG = "CFingerWriter";



    public CFingerWriter(Context context) {
        super(context);
        init(context, null);
    }

    public CFingerWriter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CFingerWriter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    /**
     * Common object initialization for all constructors
     *
     */
    private void init(Context context, AttributeSet attrs ) {
        mContext = context;

        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CStimResp,
                    0, 0);

            try {
                mLinkedViewID = a.getResourceId(R.styleable.CStimResp_linkedView, 0);
            } finally {
                a.recycle();
            }
        }

        // Create a paint object to deine the line parameters
        mPaint = new Paint();

        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4);
        mPaint.setAntiAlias(true);

        // Create a paint object to deine the line parameters
        mPaintBase = new Paint();

        mPaintBase.setColor(getResources().getColor(R.color.fingerWriterBackground));
        mPaintBase.setStyle(Paint.Style.STROKE);
        mPaintBase.setStrokeJoin(Paint.Join.ROUND);
        mPaintBase.setStrokeWidth(6);
        mPaintBase.setAntiAlias(true);

        // Create a paint object to deine the line parameters
        mPaintUpper = new Paint();

        mPaintUpper.setColor(getResources().getColor(R.color.fingerWriterBackground));
        mPaintUpper.setStyle(Paint.Style.STROKE);
        mPaintUpper.setStrokeJoin(Paint.Join.ROUND);
        mPaintUpper.setStrokeWidth(6);
        mPaintUpper.setAlpha(100);
        mPaintUpper.setPathEffect(new DashPathEffect(new float[]{25f, 12f}, 0f));
        mPaintUpper.setAntiAlias(true);

        _counter = new RecogDelay(RECDELAY, RECDELAYNT);

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(getContext());

        // Initialize the path object
        clear();
        //this.setOnTouchListener(this);
    }


    /**
     * Add Root vector to path
     *
     * @param x
     * @param y
     */
    private void startTouch(float x, float y) {
        PointF touchPt;

        if(!_touchStarted) {
            _touchStarted = true;
            applyEventNode(_onStartWriting);
        }

        if (_counter != null)
            _counter.cancel();

        if (_currentStroke == null)
            _currentStroke = new Stroke();

        touchPt = new PointF(x, y);

        _currentStroke.addPoint(touchPt);

        // Add Root node
        mPath.moveTo(x, y);

        // Track current position
        mX = x;
        mY = y;

        invalidate();
        broadcastLocation(TCONST.LOOKATSTART, touchPt);
    }


    /**
     * Return the last recognized charater in the given index
     * TODO: Manage bound violations
     *
     * @param id
     * @return
     */
    public String getRecChar(int id) {
        if(_recChars == null)
        {
            Log.d(TAG,"getRecChar is NULL - *************************");
            return "o";
        }

        Log.d(TAG, "getRecChar is - " + _recChars.get(id));
        return _recChars.get(id);
    }


    /**
     * Test whether the motion is greater than the jitter tolerance
     *
     * @param x
     * @param y
     * @return
     */
    private boolean testPointTolerance(float x, float y) {

        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        return (dx >= TOLERANCE || dy >= TOLERANCE)? true:false;
    }


    /**
     * Update the glyph path if motion is greater than tolerance - remove jitter
     *
     * @param x
     * @param y
     */
    private void moveTouch(float x, float y) {
        PointF touchPt;

        // only update if we've moved more than the jitter tolerance
        if(testPointTolerance(x,y)){

            touchPt = new PointF(x, y);

            _currentStroke.addPoint(touchPt);

            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;

            invalidate();
            broadcastLocation(TCONST.LOOKAT, touchPt);
        }
    }


    /**
     * End the current glyph path
     * TODO: Manage debouncing
     *
     */
    private void endTouch(float x, float y) {
        PointF touchPt;

        _counter.start();

        touchPt = new PointF(mX, mY);

        // Only add a new point to the glyph if it is outside the jitter tolerance
        if(testPointTolerance(x,y)) {

            _currentStroke.addPoint(touchPt);

            mPath.lineTo(x, y);
            invalidate();
        }
        broadcastLocation(TCONST.LOOKATEND, touchPt);

        // We always add the next stoke to the glyph set
        if (_currentStrokeStore == null)
            _currentStrokeStore = new ArrayList<Stroke>();

        _currentStrokeStore.add(_currentStroke);

        // TODO: to emulate current operation we store everything in a single stroke.
        //       This is not the intended use - we should clear it and start over
        //       also we should pass the currentStrokeStore to the recognizer not a single stroke

        _currentStroke = null;
    }


    public boolean onTouch(View view, MotionEvent event) {
        long   delta;
        final int action = event.getAction();

        // TODO: switch back to setting onTouchListener
        if(_enabled) {
            super.onTouchEvent(event);

            // inhibit input while the recognizer is thinking
            //
            if (!_isRecognizing) {

                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        _prevTime = _time = System.nanoTime();
                        startTouch(x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        _time = System.nanoTime();
                        moveTouch(x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        _time = System.nanoTime();
                        endTouch(x, y);
                        break;
                }
                delta = _time - _prevTime;

                //Log.i(TAG, "Touch Time: " + _time + "  :  " + delta);
                return true;

            }
        }
        return false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        float topLine  = getHeight() / 3;
        float baseLine = topLine * 2;

        Path linePath = new Path();
        linePath.moveTo(0, topLine);
        linePath.lineTo(getWidth(), topLine);

        canvas.drawPath(linePath,  mPaintUpper);
        canvas.drawLine(0, baseLine, getWidth(), baseLine, mPaintBase);

        // Immediate mode graphics -
        // Redraw the current path
        canvas.drawPath(mPath, mPaint);
    }


    private void broadcastLocation(String Action, PointF touchPt) {

        if(_watchable) {
            getLocationOnScreen(_screenCoord);

            // Let the persona know where to look
            Intent msg = new Intent(Action);
            msg.putExtra(TCONST.SCREENPOINT, new float[]{touchPt.x + _screenCoord[0], (float) touchPt.y + _screenCoord[1]});

            bManager.sendBroadcast(msg);
        }
    }


    private void clear() {

        // Create a path object to hold the vector stream
        mPath               = new Path();

        _currentStroke      = null;
        _currentStrokeStore = null;

        invalidate();
    }


    /**
     * pass results to linked view
     * @param linkedViewID
     */
    protected void updateLinkedView(int linkedViewID) {
        // Called polymorphically - subclass handles
    }


    /**
     * This is how recognition is initiated - if the user stops writing for a
     * predefined period we assume they are finished writing.
     */
    public class RecogDelay extends CountDownTimer {

        public RecogDelay(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            _isRecognizing = true;
            Log.i(TAG, "Recognition Done");

            // Tasks can only run once so create a new one for each recognition task.
            _recThread = new RecognizerThread();
            _recThread.execute();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.i(TAG, "Tick Done");
        }

    }


    /**
     * The RecognizerThread provides a background thread on which to do the rocognition task
     * TODO: We may need a scrim on the UI thread depending on the observed performance
     */
    class RecognizerThread extends AsyncTask<Void, Void, String> {


        RecognizerThread() {
        }


        public void setStrokes(Stroke[] _recognitionStrokes) {
            _recStrokes = _recognitionStrokes;
        }


        /** This is processed on the background thread - when it returns OnPostExecute is called or
        // onCancel if it was cancelled -
        */
        @Override
        protected String doInBackground(Void... unsued) {
            _recResults = _recognizer.recognize(_recStrokes);

            return null;
        }


        /** OnPostExecute is guaranteed to run on the UI thread so we can update the view etc
         * TODO: update this to do something useful
         * TODO: fix the way we process the constraint.
        */
        @Override
        protected void onPostExecute(String sResponse) {

            clear();
            Pattern pattern = Pattern.compile(_constraint);

            for (RecResult result : _recResults) {
                Log.e("jni", "ShapeID = " + result.Id + " Confidence = " + result.Confidence);
            }

            _recStrokes = null;
            _recChars   = new ArrayList<String>();

            for (int i = 0; i < _recResults.length ; i++) {
               String recChar = _recognizer.getSymbolName(_recResults[i].Id, _configFolder);

                Matcher matcher = pattern.matcher(recChar);

                if(matcher.find()) {
                    _recChars.add(recChar);
                }
            }

            _isRecognizing = false;

            // Don't do any processing if there isn't a hypothesis

            if(_recChars.size() > 0) {

                // send the result to the stimresp that is linked
                updateLinkedView(mLinkedViewID);

                // Let anyone interested know there is a new recognition set available
                bManager.sendBroadcast(new Intent(RECMSG));

                // Do any on rec behaviors
                applyEventNode(_onRecognition);
            }
        }


        /**
         *  The recognizer expects an "array" of strokes so we generate that here in the UI thread
         *  from the ArrayList of captured strokes.
         *
         */
        @Override
        protected void onPreExecute() {

            _recStrokes = new Stroke[_currentStrokeStore.size()];

            for (int s = 0; s < _currentStrokeStore.size(); s++)
                _recStrokes[s] = _currentStrokeStore.get(s);
        }
    }



    //************************************************************************
    //************************************************************************
    // Tutor methods  Start


    /**
     * TODO: rewrite the LTK project format
     * @param recogId
     */
    public void setRecognizer(String recogId) {

        _initialized = false;
        _constraint = ".";          // By default don't filter anything

        // Initialize lipitk
        File externalFileDir = getContext().getExternalFilesDir(null);
        String path = externalFileDir.getPath();

        Log.d("JNI", "Path: " + path);

        try {
            _recognizer   = new LipiTKJNIInterface(path, recogMap.get(recogId));
            _configFolder = _recognizer.getLipiDirectory() + folderMap.get(recogId);

            _recognizer.initialize();
            _initialized = true;
        }
        catch(Exception e)
        {
            // TODO: Manage initialization errors more effectively
            Log.d(TAG, "Cannot create Recognizer - Error:1");
            System.exit(1);
        }

        // reset the internal state
        clear();
    }


    /**
     * TODO: rewrite the LTK project format
     * @param recogId
     */
    public void setRecognizer(String recogId, String subset) {

        // Add a regex that will filter the recognized input.
        //
        setRecognizer(recogId);
        _constraint = subset;
    }


    /**
     * Enable or Disable the finger writer
     * @param enableState
     */
    protected void enableFW(Boolean enableState) {

        if(_initialized) {

            _enabled = enableState;

            if (enableState) {
                // Reset the flag so onStartWriting events will fire
                _touchStarted = false;
                this.setOnTouchListener(this);

            } else {
                this.setOnTouchListener(null);
            }
        }
    }


    /**
     * Enable or disable persona messages.  Whether or not the persona will
     * watch finger motion
     *
     * @param watchEnabled
     */
    protected void personaWatch(Boolean watchEnabled) {

        _watchable = watchEnabled;
    }


    public void onStartWriting(String symbol) {
        _onStartWriting = symbol;
    }


    public void onRecognitionComplete(String symbol) {
        _onRecognition = symbol;
    }


    // Must override in TClass
    // TClass domain where TScope lives providing access to tutor scriptables
    //
    protected void applyEventNode(String nodeName) {
    }


    // Tutor methods  End
    //************************************************************************
    //************************************************************************

}
