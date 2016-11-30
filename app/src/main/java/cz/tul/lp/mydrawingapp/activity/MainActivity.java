package cz.tul.lp.mydrawingapp.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.improvelectronics.sync.android.SyncCaptureReport;
import com.improvelectronics.sync.android.SyncPath;
import com.improvelectronics.sync.android.SyncStreamingListener;
import com.improvelectronics.sync.android.SyncStreamingService;

import java.util.List;
import java.util.UUID;

import cz.tul.lp.mydrawingapp.R;
import cz.tul.lp.mydrawingapp.view.DrawingView;

public class MainActivity extends Activity implements View.OnClickListener, SyncStreamingListener {

    //custom drawing view
    private DrawingView drawView;
    //buttons
    private ImageButton currPaint, drawBtn, eraseBtn, newBtn, opacityBtn, saveBtn;
    //sizes
    private float smallBrush, mediumBrush, largeBrush;

    private SyncStreamingService mStreamingService;
    private boolean mStreamingServiceBound;
    private TextView xTextView, yTextView, pressureTextView, stylusDownTetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get drawing view
        drawView = (DrawingView)findViewById(R.id.drawing);

        //get the palette and first color button
        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);
        currPaint = (ImageButton)paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed, null));

        // Bind to the ftp service.
        Intent intent = new Intent(this, SyncStreamingService.class);
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //sizes from dimensions
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        //draw button
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);

        //set initial size
        drawView.setBrushSize(mediumBrush);

        //erase button
        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        //new button
        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        //opacity
        opacityBtn = (ImageButton)findViewById(R.id.opacity_btn);
        opacityBtn.setOnClickListener(this);

        //save button
        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //user clicked paint
    public void paintClicked(View view){
        //use chosen color

        //set erase false and etc.
        drawView.setErase(false);
        drawView.setPaintAlpha(100);
        drawView.setBrushSize(drawView.getLastBrushSize());

        if(view!=currPaint){
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            //update ui
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.draw_btn:     //draw button clicked
                toolDialog(false);  //eraser false
                break;
            case R.id.erase_btn:    //eraser button clicked
                toolDialog(true);   //eraser true
                break;
            case R.id.new_btn:      //new button
                AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
                newDialog.setTitle("New drawing");
                newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
                newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        drawView.startNew();
                        dialog.dismiss();
                    }
                });
                newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        dialog.cancel();
                    }
                });
                newDialog.show();
                break;
            case R.id.save_btn://save drawing
                AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
                saveDialog.setTitle("Save drawing");
                saveDialog.setMessage("Save drawing to device Gallery?");
                saveDialog.setPositiveButton("png", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        //save drawing
                        drawView.setDrawingCacheEnabled(true);
                        //attempt to save
                        String imgSaved = MediaStore.Images.Media.insertImage(
                                getContentResolver(), drawView.getDrawingCache(),
                                UUID.randomUUID().toString()+".png", "drawing");
                        //feedback
                        if(imgSaved!=null){
                            Toast savedToast = Toast.makeText(getApplicationContext(),
                                    "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                            savedToast.show();
                        }
                        else{
                            Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                    "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                            unsavedToast.show();
                        }
                        drawView.destroyDrawingCache();
                    }
                });
                saveDialog.setNeutralButton("pdf", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        //save drawing
                        drawView.setDrawingCacheEnabled(true);

                        PdfDocument document = new PdfDocument();
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600,300, 1).create();
                        PdfDocument.Page page = document.startPage(pageInfo);
                        View content = findViewById(R.id.drawing);
                        content.draw(page.getCanvas());
                        document.finishPage(page);
                        //attempt to save
                        String imgSaved = MediaStore.Images.Media.insertImage(
                                getContentResolver(), drawView.getDrawingCache(),
                                UUID.randomUUID().toString()+".png", "drawing");
                        //feedback
                        if(imgSaved!=null){
                            Toast savedToast = Toast.makeText(getApplicationContext(),
                                    "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                            savedToast.show();
                        }
                        else{
                            Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                    "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                            unsavedToast.show();
                        }
                        drawView.destroyDrawingCache();
                    }
                });
                saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        dialog.cancel();
                    }
                });
                saveDialog.show();
                break;
        }
        if(view.getId()==R.id.opacity_btn){
            //launch opacity chooser
            final Dialog seekDialog = new Dialog(this);
            seekDialog.setTitle("Opacity level:");
            seekDialog.setContentView(R.layout.opacity_chooser);
            //get ui elements
            final TextView seekTxt = (TextView)seekDialog.findViewById(R.id.opq_txt);
            final SeekBar seekOpq = (SeekBar)seekDialog.findViewById(R.id.opacity_seek);
            //set max
            seekOpq.setMax(100);
            //show current level
            int currLevel = drawView.getPaintAlpha();
            seekTxt.setText(currLevel+"%");
            seekOpq.setProgress(currLevel);
            //update as user interacts
            seekOpq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekTxt.setText(Integer.toString(progress)+"%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}

            });
            //listen for clicks on ok
            Button opqBtn = (Button)seekDialog.findViewById(R.id.opq_ok);
            opqBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setPaintAlpha(seekOpq.getProgress());
                    seekDialog.dismiss();
                }
            });
            //show dialog
            seekDialog.show();
        }

    }

    private void toolDialog(final boolean erease) {
        final Dialog widthDialog = new Dialog(this);
        if (erease)
            widthDialog.setTitle("Brush size:");
        else
            widthDialog.setTitle("Rubber size:");
        widthDialog.setContentView(R.layout.brush_chooser);

        //listen for clicks on size buttons
        ImageButton smallBtn = (ImageButton) widthDialog.findViewById(R.id.small_brush);
        smallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeBrush(smallBrush, erease);
                widthDialog.dismiss();
            }
        });
        ImageButton mediumBtn = (ImageButton) widthDialog.findViewById(R.id.medium_brush);
        mediumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeBrush(mediumBrush, erease);
                widthDialog.dismiss();
            }
        });
        ImageButton largeBtn = (ImageButton) widthDialog.findViewById(R.id.large_brush);
        largeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeBrush(largeBrush, erease);
                widthDialog.dismiss();
            }
        });
        //show and wait for user interaction
        widthDialog.show();
    }

    private void changeBrush(float brushSize, boolean erease) {
        drawView.setErase(erease);
        drawView.setBrushSize(brushSize);
        if (!erease)
            drawView.setLastBrushSize(brushSize);
    }

    //send trackball event to drawing view
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        drawView.requestFocus();
        return drawView.onTrackballEvent(ev);
    }

    @Override
    public void onStart() {
        super.onStart();

        xTextView = (TextView)findViewById(R.id.xTextView);
//        yTextView = (TextView)findViewById(R.id.yTextView);
//        pressureTextView = (TextView)findViewById(R.id.pressureTextView);
//        stylusDownTetView = (TextView)findViewById(R.id.stylusDownTextView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStreamingServiceBound) {
            // Put the Boogie Board Sync back into MODE_NONE.
            // This way it doesn't use Bluetooth and saves battery life.
            if(mStreamingService.getState() == SyncStreamingService.STATE_CONNECTED) mStreamingService.setSyncMode(SyncStreamingService.MODE_NONE);

            // Don't forget to remove the listener and unbind from the service.
            mStreamingService.removeListener(this);
            this.unbindService(mConnection);
        }
    }

    @Override
    public void onStreamingStateChange(int prevState, int newState) {
        // Put the streaming service in capture mode to get data from Boogie Board Sync.
        if(newState == SyncStreamingService.STATE_CONNECTED) {
            mStreamingService.setSyncMode(SyncStreamingService.MODE_CAPTURE);
        }
    }

    @Override
    public void onErase() {
        Toast.makeText(this, "Erase button pushed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSave() {
        Toast.makeText(this, "Save button pushed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDrawnPaths(List<SyncPath> paths) {

    }

    @Override
    public void onCaptureReport(SyncCaptureReport captureReport) {
        xTextView.setText(captureReport.getX() + "");
//        yTextView.setText(captureReport.getY() + "");
//        pressureTextView.setText(captureReport.getPressure() + "");
//        if(captureReport.hasTipSwitchFlag()) stylusDownTetView.setVisibility(View.VISIBLE);
//        else stylusDownTetView.setVisibility(View.GONE);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Set up the service
            mStreamingServiceBound = true;
            SyncStreamingService.SyncStreamingBinder binder = (SyncStreamingService.SyncStreamingBinder) service;
            mStreamingService = binder.getService();
            mStreamingService.addListener(MainActivity.this);// Add listener to retrieve events from streaming service.

            // Put the streaming service in capture mode to get data from Boogie Board Sync.
            if(mStreamingService.getState() == SyncStreamingService.STATE_CONNECTED) {
                mStreamingService.setSyncMode(SyncStreamingService.MODE_CAPTURE);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mStreamingService = null;
            mStreamingServiceBound = false;
        }
    };

}
