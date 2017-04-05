package com.vladimircvetanov.smartfinance;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.vladimircvetanov.smartfinance.db.DBAdapter;
import com.vladimircvetanov.smartfinance.message.Message;

public class RegisterActivity extends AppCompatActivity {

    private EditText userEmail;
    private EditText userPass;
    private EditText confirmPass;
    private Button signUp;
    private Button cancel;

    private DBAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        adapter = DBAdapter.getInstance(this);

        userEmail = (EditText)findViewById(R.id.register_email_insert);
        userPass = (EditText)findViewById(R.id.register_pass_insert);
        confirmPass = (EditText)findViewById(R.id.register_confirm_insert);

        signUp = (Button)findViewById(R.id.register_reg_button);
        cancel = (Button)findViewById(R.id.register_cancel_button);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
            }
        });
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(signUp()){
                   startActivity(new Intent(RegisterActivity.this,MainActivity.class));
               }

            }
        });
    }

    private boolean signUp() {
        String username = userEmail.getText().toString();
        String pass = userPass.getText().toString();
        String confirm = confirmPass.getText().toString();

       long id = adapter.insertData(username,pass);
        if(id < 0 ){
            Message.message(this,"No such user!");
            return false ;
        }
        if(username.isEmpty()){
            userEmail.setError("Empty email");
            userEmail.requestFocus();
            return false;
        }
        if(pass.isEmpty()){
            userPass.setError("Empty password");
            userPass.requestFocus();
            return false;
        }
        if(confirm.isEmpty()){
            confirmPass.setError("Empty confirmation");
            confirmPass.requestFocus();
            return false;
        }
        if(!pass.equals(confirm)){

            confirmPass.setError("Different passwords");
            confirmPass.setText("");
            confirmPass.requestFocus();
            return false;
        }
        Message.message(this,"User registered!");

        return true;


    }
}