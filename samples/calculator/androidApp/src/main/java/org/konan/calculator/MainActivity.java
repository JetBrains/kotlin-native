package org.konan.calculator;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.konan.arithmeticparser.ParserKt;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView resultView = findViewById(R.id.computed_result);

        final EditText input = findViewById(R.id.input);
        input.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                final String inputText = v.getText().toString();
                final Double result = ParserKt.parseAndCompute(inputText).getExpression();
                if (result != null) {
                    resultView.setText(inputText + " = " + result.toString());
                } else {
                    resultView.setText("Unable to parse " + inputText);
                }
                return true;
            }
        });
    }
}
