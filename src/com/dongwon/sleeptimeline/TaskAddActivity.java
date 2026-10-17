package com.dongwon.sleeptimeline;
import android.app.*;import android.graphics.Color;import android.os.*;import android.text.InputType;import android.view.*;import android.view.inputmethod.EditorInfo;import android.widget.*;
/* 위젯의 + 버튼. 작은 입력창만 띄우고 저장 후 바로 닫힌다. */
public class TaskAddActivity extends Activity {
 EditText input;TextView error;
 public void onCreate(Bundle state){
  super.onCreate(state);float d=getResources().getDisplayMetrics().density;
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding((int)(20*d),(int)(18*d),(int)(20*d),(int)(14*d));
  TextView title=new TextView(this);title.setText("할 일 추가");title.setTextColor(Color.WHITE);title.setTextSize(19);title.setTypeface(null,android.graphics.Typeface.BOLD);box.addView(title);
  input=new EditText(this);input.setHint("예: 창문에 검은 네모 붙이기");input.setTextColor(Color.rgb(242,242,236));input.setHintTextColor(Color.rgb(120,122,108));input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setImeOptions(EditorInfo.IME_ACTION_DONE);
  input.setOnEditorActionListener(new TextView.OnEditorActionListener(){public boolean onEditorAction(TextView v,int action,KeyEvent e){if(action==EditorInfo.IME_ACTION_DONE){save();return true;}return false;}});
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=(int)(12*d);box.addView(input,lp);
  error=new TextView(this);error.setTextColor(Color.rgb(255,179,184));error.setTextSize(12);box.addView(error);
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.END);
  Button cancel=button("취소",Color.rgb(191,192,168));cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){finish();}});
  Button add=button("추가",Color.rgb(214,208,110));add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){save();}});
  row.addView(cancel);row.addView(add);box.addView(row);
  setContentView(box);
  getWindow().setLayout((int)Math.min(getResources().getDisplayMetrics().widthPixels-32*d,380*d),ViewGroup.LayoutParams.WRAP_CONTENT);
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);input.requestFocus();
 }
 Button button(String text,int color){Button b=new Button(this,null,android.R.attr.borderlessButtonStyle);b.setText(text);b.setTextColor(color);b.setTextSize(15);return b;}
 void save(){
  try{TaskStore.get(this).add(input.getText().toString(),System.currentTimeMillis());}catch(Exception e){error.setText(e.getMessage());return;}
  TodoWidget.refreshAll(this);finish();
 }
}
