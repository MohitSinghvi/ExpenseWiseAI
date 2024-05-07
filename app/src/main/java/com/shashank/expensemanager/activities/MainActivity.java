package com.shashank.expensemanager.activities;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.view.View;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.adapters.SectionsPageAdapter;
import com.shashank.expensemanager.fragments.BalanceFragment;
import com.shashank.expensemanager.fragments.ChatbotFragment;
import com.shashank.expensemanager.fragments.CustomBottomSheetDialogFragment;
import com.shashank.expensemanager.fragments.ExpenseFragment;
import com.shashank.expensemanager.fragments.UploadFragment;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;

    public static FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mViewPager=findViewById(R.id.container);
        setupViewPager(mViewPager);

        TabLayout tabLayout=findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


         fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CustomBottomSheetDialogFragment().show(getSupportFragmentManager(), "Dialog");

            }
        });

    }




    private void setupViewPager(ViewPager viewPager){
        SectionsPageAdapter adapter=new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new ExpenseFragment(),"Expenses");
        adapter.addFragment(new BalanceFragment(),"Balance");
        adapter.addFragment(new ChatbotFragment(), "AI CHAT");
        adapter.addFragment(new UploadFragment(), "AI VISION");
        viewPager.setAdapter(adapter);
    }



}
