package com.example.dbdesign;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.dbdesign.db.BaseDao;
import com.example.dbdesign.db.DaoSplitFactory;
import com.example.dbdesign.db.DbPath;
import com.example.dbdesign.db.UpdateManager;
import com.example.dbdesign.db.UserDao;
import com.example.dbdesign.db.bean.Address;
import com.example.dbdesign.db.bean.User;
import com.example.dbdesign.utils.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                1);
        TextView textView = findViewById(R.id.textView);

        Button move = findViewById(R.id.move);
        move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int byteRead = 0;
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = getAssets().open("updateXml.xml");
                    File file = new File(DbPath.LOCAL_UPGARDE_FILE_PATH);
                    if (!file.exists()) {
                        File parent = file.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                        file.createNewFile();
                    }
                    outputStream = new FileOutputStream(file);
                    byte[] bytes = new byte[1024];
                    while ((byteRead = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, byteRead);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                File file = new File(DbPath.LOCAL_UPGARDE_FILE_PATH);
                if (file.exists()) {
                    textView.setText("updateXml.xml moved success");
                }
            }
        });

        Button insert = findViewById(R.id.insert);
        insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDao userDao = DaoSplitFactory.getInstance().getSubDao(UserDao.class, User.class);
                userDao.insetEntity(new User(1, "NIHAO1", "ABC", 0));
                userDao.insetEntity(new User(2, "NIHAO2", "ABCD", 0));
                userDao.insetEntity(new User(3, "NIHAO3", "ABCDF", 1));
                userDao.insetEntity(new User(4, "NIHAO4", "ABCDFE", 0));
                textView.setText("插入成功");
            }
        });

        Button query = findViewById(R.id.query);
        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDao userDao = DaoSplitFactory.getInstance().getSubDao(UserDao.class, User.class);
                List<User> list = userDao.query(new User());
                StringBuilder longString = new StringBuilder();
                if (list == null) {
                    textView.setText("user 表数据空");
                    return;
                }
                for (User user : list) {
                    StringBuilder stringBuilder = new StringBuilder("user:");
                    stringBuilder.append(" Id:");
                    stringBuilder.append(user.getId());
                    stringBuilder.append(", Name:");
                    stringBuilder.append(user.getName());
                    stringBuilder.append(", Password:");
                    stringBuilder.append(user.getPassword());
                    stringBuilder.append(", Status:");
                    stringBuilder.append(user.getStatus());
                    longString.append(stringBuilder.toString());
                    longString.append("\n");
                }

                textView.setText(longString.toString());
            }
        });

        Button update = findViewById(R.id.update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDao userDao = DaoSplitFactory.getInstance().getSubDao(UserDao.class, User.class);
                userDao.updateEntity(new User(5, "NIHAO5", "ABC", 0) ,
                        new User(2, "NIHAO2", "ABCD", 0));
                textView.setText("跟新成功");
            }
        });

        Button delete = findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDao userDao = DaoSplitFactory.getInstance().getSubDao(UserDao.class, User.class);
                userDao.delete(new User());
                textView.setText("删除成功");
            }
        });

        Button upgrade = findViewById(R.id.upgrade);
        upgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(DbPath.LOCAL_UPGARDE_FILE_PATH);
                if (!file.exists())
                  FileUtil.writeFile("V1",DbPath.LOCAL_UPGARDE_FILE_PATH,false);
                UpdateManager updateManager = new UpdateManager();
                int resultFlag = updateManager.executeUpdateDb(UpdateManager.MODEL_CREATE_UPGRADE);
                if (resultFlag == 1)
                    textView.setText("1 升级忽略");
                else if (resultFlag == 0)
                    textView.setText("0 升级成功");
                else if (resultFlag == -1)
                    textView.setText("-1 升级失败");
            }
        });
    }
}
