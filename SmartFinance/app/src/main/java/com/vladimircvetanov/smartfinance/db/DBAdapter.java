package com.vladimircvetanov.smartfinance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.widget.Toast;

import com.vladimircvetanov.smartfinance.message.Message;
import com.vladimircvetanov.smartfinance.model.LogEntry;
import com.vladimircvetanov.smartfinance.model.Manager;
import com.vladimircvetanov.smartfinance.model.Section;
import com.vladimircvetanov.smartfinance.model.User;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.HashMap;

import static android.R.attr.category;

/**
 * Created by vladimircvetanov on 04.04.17.
 */

public class DBAdapter {

    Context context;
    /**
     * Declaration of fields of the adapter class. A reference to innerclass will executes queries.
     */
    static DbHelper helper ;

    private static HashMap<String,User> registeredUsers;
    /**
     * Static reference to the instance of the adapter.Private static because helps to create only one instance of type DbAdapter.
     */
    private static DBAdapter instance = null;
    /**
     * Constructor of the Adapter class. An object with reference to the inner class is instanciated with a context parameter.
     * The adapter creates and performs database management tasks.
     *
     * @param context
     */
    private DBAdapter(Context context){

        helper = new DbHelper(context);
        this.context = context;


    }

    /**
     * Static method which returns single instance of the DbAdapter.
     * @return
     */
    public static DBAdapter getInstance(Context context){
        if(instance == null){
            instance = new DBAdapter(context);
            registeredUsers = new HashMap<>();
            loadUsers();

        }
        return instance;
    }

    /**
     * A method used to insert data in the database.
     * @param username
     * @param password
     * @return long id if the method was successful and -1 if it fails
     */
    public long insertData(final String username,final String password){

        final long[] id = new long[1];
        final boolean[] flag = new boolean[1];

        new AsyncTask<Long,Void,Void>() {



            @Override
            protected Void doInBackground(Long... params) {

                if (!flag[0]) {
                    /**
                     *  A reference from inner class is used to create a Database object.
                     */
                    SQLiteDatabase db = helper.getWritableDatabase();

                    /**
                     * An instance of ContentValues class is created. To insert data the reference takes a key and a value.
                     * We specify the key as the column name. The value is the data we want ot put inside.
                     */
                    ContentValues values = new ContentValues();

                    /**
                     * Three columns are inserted;
                     */

                    values.put(DbHelper.COLUMN_USERNAME, username);
                    values.put(DbHelper.COLUMN_PASSWORD, password);

                    /**
                     * The insert method with three parameters(String TableName,String NullColumnHack,ContentValues values)
                     * is called on the SQL object of the class.
                     * It returns the ID of the inserted row or -1 if the operation fails.
                     */
                    id[0] = db.insert(DbHelper.TABLE_NAME_USERS, null, values);



                }

                return null;
            }

        }.execute();
            return id[0];
    }
    private  String getData(final String username){

        /**
         *  A reference from inner class is used to create a Database object.
         */
        SQLiteDatabase db = helper.getWritableDatabase();

        /**
         * select username,pass from SmartFinance table.
         */
        String[] columns = {DbHelper.COLUMN_USERNAME,DbHelper.COLUMN_PASSWORD};

        /**
         * object which contains username and password of the user.
         */
        StringBuffer buffer = new StringBuffer();

        /**
         * A call to the query method. Cursor object returned by the query method.
         * The cursor object's reference is the control which let's us move from the top to the bottom
         * of the table's result sets.
         * The method query takes seven parameters:
         * String table, String[] columns (list of columns to process, null returns all);
         * extra conditions on the SQL statement to return rows satisfying certain criteria,
         * String selection, String [] selectionArgs, String groupBy, String having, String orderby
         */
        Cursor cursor = db.query(DbHelper.TABLE_NAME_USERS,columns,DbHelper.COLUMN_USERNAME + " = '" + username + "'",null,null,null,null);


        while(cursor.moveToNext()){
            int index1 = cursor.getColumnIndex(DbHelper.COLUMN_USERNAME);
            int index2 = cursor.getColumnIndex(DbHelper.COLUMN_PASSWORD);

            String personUsername = cursor.getString(index1);
            String personPass = cursor.getString(index2);

            buffer.append(personUsername + " " + personPass + "\n");
        }

        return buffer.toString();

    }

    public long getUserId(String email){

        SQLiteDatabase db = helper.getReadableDatabase();

        String[] columns = {DbHelper.COLUMN_ID};
        String[] params = {email};
        Cursor cursor = db.rawQuery(" SELECT " + DbHelper.COLUMN_ID + " FROM " + DbHelper.TABLE_NAME_USERS + " WHERE username =?",params);

        if(cursor.moveToNext()){
           int columnIndex = cursor.getColumnIndex(DbHelper.COLUMN_ID);
           return cursor.getInt(columnIndex);
        }
        return -1;
    }
    /**
     * Method to verify user data if user is already in database.
     *
     * @param username
     * @param password
     * @return
     */
    public  boolean getUser(String username,String password){
        String details = username + " " + password + "\n";

        if(details.equals(getData(username))){
            return true;
        }
        return false;
    }

    private static void loadUsers(){

        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = helper.getWritableDatabase().rawQuery("SELECT _id,username,password FROM Users;",null);
                while(cursor.moveToNext()){
                    int id = cursor.getInt(cursor.getColumnIndex("_id"));
                    String email = cursor.getString(cursor.getColumnIndex("username"));
                    String pass = cursor.getString(cursor.getColumnIndex("password"));
                    User u = new User(email,pass);
                    u.setId(id);
                    registeredUsers.put(email,u);
                }
                return null;
            }
        }.execute();


    }
    public boolean existsUser(String username){
        return registeredUsers.containsKey(username);
    }

    public void updateUser(String oldEmail, String oldPass, final String newEmail, final String newPass) {

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
               SQLiteDatabase db = helper.getWritableDatabase();
                String oldEmail = strings[0];
                User u = registeredUsers.get(oldEmail);
                //update

                ContentValues values = new ContentValues();
                values.put("username", newEmail);
                values.put("password", newPass);

                u.setEmail(newEmail);
                u.setPassword(newPass);
                registeredUsers.remove(oldEmail);
                registeredUsers.put(newEmail, u);
                db.update("Users", values, "username = ?", new String[]{oldEmail});

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(context, " user edited successfully", Toast.LENGTH_SHORT).show();
            }
        }.execute(oldEmail);

    }


    public void getAllAccounts(){

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                String[] columns = {DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME,DbHelper.ACCOUNTS_COLUMN_ICON};

                Cursor cursor = db.query(DbHelper.TABLE_NAME_ACCOUNTS,columns,null,null,null,null,null);

                while(cursor.moveToNext()){

                    int index = cursor.getColumnIndex(DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME);
                    int index2 =cursor.getColumnIndex(DbHelper.ACCOUNTS_COLUMN_ICON);
                    String name = cursor.getString(index);
                    int icon = cursor.getInt(index2);

                    Manager.addSection(new Section(name, Manager.Type.INCOMING,icon));

                }
                return null;
            }
        }.execute();

    }

    public long addAccount(final Section account,final int userId){
        final long[] id = new long[1];

       new AsyncTask<Void,Void,Void>(){
           @Override
           protected Void doInBackground(Void... params) {
               if(!Manager.containsSection(account)) {
                   SQLiteDatabase db = helper.getWritableDatabase();

                   ContentValues values = new ContentValues();

                   values.put(DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME,account.getName());
                   values.put(DbHelper.ACCOUNTS_COLUMN_ICON,account.getIconID());
                   values.put(DbHelper.ACCOUNTS_COLUMN_USERFK,userId);

                   id[0] = db.insert(DbHelper.TABLE_NAME_ACCOUNTS,null,values);
               }
               return null;
           }
       }.execute();

        return id[0];
    }

    public int deleteAccount(final Section account){

        final int[] count = new int[1];

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                count[0] = db.delete(DbHelper.TABLE_NAME_ACCOUNTS,DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME + " = " + account.getName(),null);
                return null;
            }

            @Override
            protected void onPostExecute(Void integer) {
                Message.message(context,"Account deleted!");
            }
        }.execute();
        return count[0];
    }

    public void getAllCategories(){

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                String[] columns = {DbHelper.CATEGORIES_COLUMN_CATEGORYNAME,DbHelper.CATEGORIES_COLUMN_ICON};

                Cursor cursor = db.query(DbHelper.TABLE_NAME_CATEGORIES,columns,null,null,null,null,null);

                while(cursor.moveToNext()){

                    int index = cursor.getColumnIndex(DbHelper.CATEGORIES_COLUMN_CATEGORYNAME);
                    int index2 =cursor.getColumnIndex(DbHelper.CATEGORIES_COLUMN_ICON);
                    String name = cursor.getString(index);
                    int icon = cursor.getInt(index2);

                    Manager.addSection(new Section(name, Manager.Type.EXPENSE,icon));

                }
                return null;
            }
        }.execute();

    }
    public long addCategory(final Section category,final int userId){
        final long[] id = new long[1];

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                if(!Manager.containsSection(category)) {
                    SQLiteDatabase db = helper.getWritableDatabase();

                    ContentValues values = new ContentValues();

                    values.put(DbHelper.CATEGORIES_COLUMN_CATEGORYNAME,category.getName());
                    values.put(DbHelper.CATEGORIES_COLUMN_ICON,category.getIconID());
                    values.put(DbHelper.CATEGORIES_COLUMN_USERFK,userId);

                    id[0] = db.insert(DbHelper.TABLE_NAME_CATEGORIES,null,values);
                }
                return null;
            }
        }.execute();

        return id[0];
    }
    public int deleteCategory(final Section category){
        final int[] count = new int[1];

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                count[0] = db.delete(DbHelper.TABLE_NAME_CATEGORIES,DbHelper.CATEGORIES_COLUMN_CATEGORYNAME + " = " + category.getName(),null);
                return null;
            }

            @Override
            protected void onPostExecute(Void integer) {
                Message.message(context,"Category deleted!");
            }
        }.execute();
        return count[0];
    }

    public void getAllFavCategories(){

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                String[] columns = {DbHelper.FAVCATEGORIES_COLUMN_CATEGORYNAME,DbHelper.FAVCATEGORIES_COLUMN_ICON};

                Cursor cursor = db.query(DbHelper.TABLE_NAME_FAVCATEGORIES,columns,null,null,null,null,null);

                while(cursor.moveToNext()){

                    int index = cursor.getColumnIndex(DbHelper.FAVCATEGORIES_COLUMN_CATEGORYNAME);
                    int index2 =cursor.getColumnIndex(DbHelper.FAVCATEGORIES_COLUMN_ICON);
                    String name = cursor.getString(index);
                    int icon = cursor.getInt(index2);

                    Manager.addSection(new Section(name, Manager.Type.EXPENSE,icon));

                }
                return null;
            }
        }.execute();

    }
    public long addFavCategory(final Section category,final int userId){
        final long[] id = new long[1];

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                if(!Manager.containsSection(category)) {
                    SQLiteDatabase db = helper.getWritableDatabase();

                    ContentValues values = new ContentValues();

                    values.put(DbHelper.FAVCATEGORIES_COLUMN_CATEGORYNAME,category.getName());
                    values.put(DbHelper.FAVCATEGORIES_COLUMN_ICON,category.getIconID());
                    values.put(DbHelper.FAVCATEGORIES_COLUMN_USERFK,userId);

                    id[0] = db.insert(DbHelper.TABLE_NAME_FAVCATEGORIES,null,values);
                }
                return null;
            }
        }.execute();

        return id[0];
    }
    public int deleteFavCategory(final Section category){
        final int[] count = new int[1];

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                count[0] = db.delete(DbHelper.TABLE_NAME_FAVCATEGORIES,DbHelper.FAVCATEGORIES_COLUMN_CATEGORYNAME + " = " + category.getName(),null);
                return null;
            }

            @Override
            protected void onPostExecute(Void integer) {
                Message.message(context,"Category deleted!");
            }
        }.execute();
        return count[0];
    }

    public long addTransaction(final LogEntry transaction, final int userId){
        final long[] id = new long[1];

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = helper.getWritableDatabase();

                ContentValues values = new ContentValues();

                String accountName = transaction.getAccount().getName();

                //Used Long wrapper class, because for income transactions, the category member is null.
                Long accountID = null;
                Long categoryID = null;

                long userID = userId;

                String[] columns = {DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME,DbHelper.COLUMN_ID};
                Cursor cursor = db.query(DbHelper.TABLE_NAME_ACCOUNTS,columns,null,null,null,null,null);

                //Get Account entry dbID
                while(cursor.moveToNext()){

                    int indexNameCol = cursor.getColumnIndex(DbHelper.ACCOUNTS_COLUMN_ACCOUNTNAME);

                    String name = cursor.getString(indexNameCol);
                    if (name.equals(accountName)) {
                        categoryID = cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_ID));
                        break;
                    }
                }
                if (accountID == null) accountID = addAccount(transaction.getAccount(),userId);

                //If transaction is an EXPENSE -> get Category entry dbID
                if (transaction.getType() == Manager.Type.EXPENSE){
                    columns = new String[] {DbHelper.CATEGORIES_COLUMN_CATEGORYNAME,DbHelper.COLUMN_ID};
                    cursor = db.query(DbHelper.TABLE_NAME_CATEGORIES,columns,null,null,null,null,null);

                    String categoryName = transaction.getAccount().getName();

                    while(cursor.moveToNext()){

                        int indexNameCol = cursor.getColumnIndex(DbHelper.CATEGORIES_COLUMN_CATEGORYNAME);

                        String name = cursor.getString(indexNameCol);
                        if (name.equals(categoryName)) {
                            categoryID = cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_ID));
                            break;
                        }
                    }
                    if (categoryID == null) categoryID = addCategory(transaction.getCategory(),userId);
                }

                values.put(DbHelper.TRANSACTIONS_COLUMN_SUM, transaction.getSum());
                values.put(DbHelper.TRANSACTIONS_COLUMN_DATE, transaction.getDate().getMillis());
                values.put(DbHelper.TRANSACTIONS_COLUMN_NOTE, transaction.getNote());

                values.put(DbHelper.TRANSACTIONS_COLUMN_ACCOUNTFK, accountID);
                values.put(DbHelper.TRANSACTIONS_COLUMN_CATEGORYFK, categoryID);
                values.put(DbHelper.TRANSACTIONS_COLUMN_USERFK, userId);

                id[0] = db.insert(DbHelper.TABLE_NAME_FAVCATEGORIES, null, values);

                return null;
            }
        }.execute();

        return id[0];
    }

    /**
     * Inner static class which is responsible for the creation of  database.
     * A custom class implementation of SQLiteOpenHelper is created. Database's schema is defined programatically.
     * This class takes care of opening the database if it exists,
     * creating it if it does not exist and upgrading it if necessary.
     */
    static class  DbHelper extends SQLiteOpenHelper {

        /**
         * Definition of unique for the application database name. Specify a String constant.
         */
        private static final String DB_NAME = "smartfinance.db";

        /**
         * Definition of the database's tables name`s. Specify a String constant.
         */
        private static final String TABLE_NAME_USERS = "Users" ;

        private static final String TABLE_NAME_ACCOUNTS = "Accounts";

        private static final String TABLE_NAME_CATEGORIES = "Categories";

        private static final String TABLE_NAME_FAVCATEGORIES = "Fav_Categories";

        private static final String TABLE_NAME_TRANSACTIONS = "Transactions";
        /**
         * Constant String SQL statement for erasing old version of table.
         */
        private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

        /**
         * Constant integer of database`s version.
         */
        private static final int DB_VERSION = 2;

        /**
         * Constant String of the table`s column for id;
         */
        private static final String COLUMN_ID = "_id";

        /**
         * Constant String of the table`s column for usernames;
         */
        private static final String COLUMN_USERNAME = "username";

        /**
         * Constant String of the table`s column for passwords;
         */
        private static final String COLUMN_PASSWORD = "password";

        private static final String ACCOUNTS_COLUMN_ACCOUNTNAME = "account_name";

        private static final String ACCOUNTS_COLUMN_ICON = "account_icon";

        private static final String ACCOUNTS_COLUMN_USERFK = "account_user";

        private static final String CATEGORIES_COLUMN_CATEGORYNAME = "category_name";

        private static final String CATEGORIES_COLUMN_USERFK = "category_user";

        private static final String CATEGORIES_COLUMN_ICON = "category_icon";

        private static final String FAVCATEGORIES_COLUMN_CATEGORYNAME = "fav_category_name";

        private static final String FAVCATEGORIES_COLUMN_USERFK = "fav_category_user";

        private static final String FAVCATEGORIES_COLUMN_ICON = "fav_category_icon";

        private static final String TRANSACTIONS_COLUMN_NOTE = "transaction_note";

        private static final String TRANSACTIONS_COLUMN_SUM = "transaction_sum";

        private static final String TRANSACTIONS_COLUMN_DATE = "transaction_date";

        private static final String TRANSACTIONS_COLUMN_USERFK = "transaction_user_fk";

        private static final String TRANSACTIONS_COLUMN_CATEGORYFK = "transaction_category_fk";

        private static final String TRANSACTIONS_COLUMN_ACCOUNTFK = "transaction_account_fk";

        /**
         * Constant String SQL statement for creating new database table.
         */
        public static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_NAME_USERS + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_USERNAME + " VARCHAR(255), " +
                COLUMN_PASSWORD + " VARCHAR(255));";

        public static final String CREATE_TABLE_ACCOUNTS = "CREATE TABLE " + TABLE_NAME_ACCOUNTS + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + ACCOUNTS_COLUMN_ACCOUNTNAME + " VARCHAR(255), " +
                ACCOUNTS_COLUMN_ICON + " INTEGER, " + ACCOUNTS_COLUMN_USERFK + " INTEGER);";

        public static final String CREATE_TABLE_CATEGORIES = "CREATE TABLE " + TABLE_NAME_CATEGORIES + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + CATEGORIES_COLUMN_CATEGORYNAME + " VARCHAR(255), " +
                CATEGORIES_COLUMN_ICON + " INTEGER, " + CATEGORIES_COLUMN_USERFK + " INTEGER);";

        public static final String CREATE_TABLE_FAVCATEGORIES = "CREATE TABLE " + TABLE_NAME_FAVCATEGORIES + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + FAVCATEGORIES_COLUMN_CATEGORYNAME + " VARCHAR(255), " +
                FAVCATEGORIES_COLUMN_ICON + " INTEGER, " + FAVCATEGORIES_COLUMN_USERFK + " INTEGER);";

        public static final String CREATE_TABLE_TRANASCTIONS = "CREATE TABLE " + TABLE_NAME_TRANSACTIONS + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + TRANSACTIONS_COLUMN_NOTE + " VARCHAR(255), " +
                TRANSACTIONS_COLUMN_SUM + " REAL, " + TRANSACTIONS_COLUMN_DATE + " INTEGER, " + TRANSACTIONS_COLUMN_CATEGORYFK + " INTEGER," +
                 TRANSACTIONS_COLUMN_USERFK + " INTEGER, " + TRANSACTIONS_COLUMN_ACCOUNTFK + " INTEGER);";


        /**
         * Definition of context;
         */
        private static Context context;





        /**
         * Declaration of constructor the supertype class.
         * Object is responsible for the creation of  single instance of database and
         * editing of database. Constructor takes context as parameter.
         * Super constructor takes four parameters(context, database name, custom cursor object and version of database.
         * Since we do not create custom cursor factory, we pass null.
         *
         * @param context
         */
       private DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            this.context = context;
        }




        /**
         * {@inheritDoc}
         * The method is called when the database is first created. Creation of tables and initial data inside tables is put here.
         *
         * @param db
         */
        @Override
        public void onCreate(SQLiteDatabase db) {

            try {
                /**
                 * Executes a single SQL statement that is NOT a SELECT and does not return data.
                 */
                db.execSQL(CREATE_TABLE_USERS);
                db.execSQL(CREATE_TABLE_ACCOUNTS);
                db.execSQL(CREATE_TABLE_CATEGORIES);
                db.execSQL(CREATE_TABLE_FAVCATEGORIES);
                db.execSQL(CREATE_TABLE_TRANASCTIONS);

            }
            /**
             * If the SQL statement is invalid it throws an exception.
             */
            catch (SQLException e) {
                Message.message(context, "" + e);
            }
        }

        /**
         * {@inheritDoc}
         * Method is called when database needs to be upgraded. It is triggered when updates are made.
         * This method is used to drop tables, add tables, do anything that needs to upgrade to new version of schema.
         * It deletes the old table and creates the new one with new parameters with query.
         *
         * @param db         - the database.
         * @param oldVersion - go from old version
         * @param newVersion - to new version.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            try {
                /**
                 * When there are edits, the old table is deleted.
                 */
                db.execSQL(DROP_TABLE + TABLE_NAME_USERS);
                db.execSQL(DROP_TABLE + TABLE_NAME_ACCOUNTS);
                db.execSQL(DROP_TABLE + TABLE_NAME_TRANSACTIONS);
                db.execSQL(DROP_TABLE + TABLE_NAME_CATEGORIES);
                db.execSQL(DROP_TABLE + TABLE_NAME_FAVCATEGORIES);

                /**
                 * Once the table is deleted the new database is created once again with new statements.
                 */
                onCreate(db);

            }
            /**
             * If the SQL statement is invalid it throws an exception.
             */ catch (SQLException e) {
                Message.message(context, "" + e);

            }
        }
    }
}
