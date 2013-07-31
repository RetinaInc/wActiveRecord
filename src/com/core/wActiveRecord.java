package com.core;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static com.core.output.Debug.log;
//import javax.persistence.Table;

/**
 *
 * @author wr
 */
public class wActiveRecord extends wQueryBuilder {

    
    /**
     * Initialize database credentials
     *
     * @param host
     * @param port
     * @param database
     * @param user
     * @param pass
     */
    public wActiveRecord(String host, String port, String database, String user, String pass) {
        this._host = host;
        this._port = port;
        this._database = database;
        this._user = user;
        this._pass = pass;

        log("Loaded");
        _connect();
        log("Connection to database successfully");
    }

    //------------------- Active Record Query -------------------
    //- SELECT
    public wActiveRecord select(String str) {
        this._select = str;
        return this;
    }

    //- FROM
    public wActiveRecord from(Class<Serializable> table) {
        this._table = table.getAnnotation(Table.class).name();
        return this;
    }

    public wActiveRecord from(String table) {
        this._table = table;
        return this;
    }

    //- Join
    public wActiveRecord join(String table, String clausure, String joinType) {
        this._join.put(table + "|" + joinType, clausure);
        return this;
    }

    public wActiveRecord join(String table, String clausure) {
        return this.join(table, clausure, "inner");
    }

    //- WHERE
    public wActiveRecord where(String column, String value, boolean scape) {
        this._where.put(column, (scape ? "'" + value + "'" : value));
        return this;
    }

    public wActiveRecord where(String column, String value) {
        return this.where(column, value, true);
    }

    public wActiveRecord where(String column, int value) {
        return this.where(column, String.valueOf(value), true);
    }

    public wActiveRecord where(String column, int value, boolean scape) {
        return this.where(column, String.valueOf(value), scape);
    }

    public wActiveRecord where_in(String column, String... clause) {
        this._where_in.put(column, clause);
        return this;
    }

    //- LIKE
    /**
     * Like closure
     *
     * @param column
     * @param value
     * @param math | L, R, [default B]: %Left, Right% OR %Both%
     * @return
     */
    public wActiveRecord like(String column, String value, String math) {

        switch (math.toUpperCase()) {
            case "L":
                value = "%" + value;
                break;
            case "R":
                value = value + "%";
                break;
            default:
                value = "%" + value + "%";
                break;
        }

        this._like.put(column, value);
        return this;
    }

    public wActiveRecord like(String column, String value) {
        return this.like(column, value, "B");
    }

    //- OR LIKE
    /**
     * Like closure
     *
     * @param column
     * @param value
     * @param math | L, R, [default B]: %Left, Right% OR %Both%
     * @return
     */
    public wActiveRecord or_like(String column, String value, String math) {

        switch (math.toUpperCase()) {
            case "L":
                value = "%" + value;
                break;
            case "R":
                value = value + "%";
                break;
            default:
                value = "%" + value + "%";
                break;
        }

        this._or_like.put(column, value);
        return this;
    }

    public wActiveRecord or_like(String column, String value) {
        return this.or_like(column, value, "B");
    }

    //- LIMIT
    public wActiveRecord limit(int limit) {
        return this.limit(limit, 0);
    }

    public wActiveRecord limit(int limit, int offset) {
        this._limit = String.valueOf(offset) + "," + String.valueOf(limit);
        return this;
    }

    //- ORDER BY
    public wActiveRecord orderBy(String syntax) {
        this._order_by = syntax;
        return this;
    }

    public wActiveRecord orderBy(String column, String value) {
        this._order_by = column + " " + value;
        return this;
    }

    //- GROUP BY
    public wActiveRecord groupBy(String syntax) {
        this._group_by = syntax;
        return this;
    }

    public wActiveRecord groupBy(String column, String value) {
        this._group_by = column + " " + value;
        return this;
    }

    /**
     * Set database prefix
     *
     * @param str
     */
    public void setDbPrefix(String str) {
        this._db_prefix = str;
    }
    
    /*public List get(Class<? extends Serializable> table) {
        return get(table.getAnnotation(Table.class).name());
    }*/
    
    /**
     * Get list of records List<HashMap<String,String>>, if not record found
     * null is returned
     *
     * @return List<HashMap<String,String>>
     */
    

    public List get() {
        return get(this._table);
    }

    /**
     * Get list of records List<HashMap<String,String>>, if not record found
     * null is returned
     *
     * @return List<HashMap<String,String>>
     */
    public List get(String table) {

        this._table = table;

        String SQL = prepareQuery();
        try {
            _resultset = _statement.executeQuery(SQL);
            List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();

            while (_resultset.next()) {

                ResultSetMetaData rsData = _resultset.getMetaData();
                HashMap columName = new HashMap();
                int totalColumn = rsData.getColumnCount();

                for (int i = 1; i <= totalColumn; i++) {
                    columName.put(rsData.getColumnLabel(i), _resultset.getString(i));
                }
                result.add(columName);
            }
            log(String.format("Query executed: \"%s\"", SQL));
            return result;

        } catch (SQLException ex) {
            log(ex.getMessage(), true);
            log(SQL, true);
            System.exit(0);
        }
        return null;
    }

    /**
     * Get result count
     *
     * @return
     */
//    public int get_count() {
//        return get_count(this._table.);
//    }
    /**
     * Get result count
     *
     * @param table
     * @return
     */
    public int get_count(Class<Serializable> table) {
        List result = this.get(table);
        if (result != null) {
            return result.size();
        }
        return 0;
    }

    private String prepareQuery() {
        Iterator it;
        String SQL = "SELECT " + this._select + " FROM " + this._table;

        // JOIN
        if (_join.size() > 0) {
            it = _join.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry map = (Map.Entry) it.next();
                // Get table and type of join... e.g: table|left
                String[] joinType = map.getKey().toString().split("\\|");
                SQL += " " + joinType[1].toUpperCase() + " JOIN " + joinType[0] + " ON " + map.getValue();
            }

        }

        boolean needAnd = false;

        // WHERE
        if (_where.size() > 0) {

            SQL += " WHERE";
            it = _where.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry map = (Map.Entry) it.next();
                SQL += (needAnd ? " AND " : " ") + map.getKey() + " = " + map.getValue();

                needAnd = true;
            }

        }

        // WHERE
        if (_where_in.size() > 0) {

            if (!needAnd) {
                SQL += " WHERE";
            }
            it = _where_in.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry map = (Map.Entry) it.next();
                StringBuilder sb = new StringBuilder();

                for (String str : _where_in.get(map.getKey())) {
                    sb.append("'" + str + "',");
                }

                sb.setLength(sb.length()-1);

                SQL += (needAnd ? " AND " : " ") + map.getKey() + " IN(" + sb.toString() + ")";
            }
            needAnd = true;
        }

        // LIKE
        if (_like.size() > 0) {

            if (!needAnd) {
                SQL += " WHERE";
            }

            it = _like.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry map = (Map.Entry) it.next();
                SQL += (needAnd ? " AND " : " ") + map.getKey() + " LIKE('" + map.getValue() + "')";
            }
            needAnd = true;
        }

        // OR LIKE
        if (_or_like.size() > 0) {

            if (!needAnd) {
                SQL += " WHERE";
            }

            it = _or_like.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry map = (Map.Entry) it.next();
                SQL += (needAnd ? " OR " : " ") + map.getKey() + " LIKE('" + map.getValue() + "')";
            }
            needAnd = true;
        }

        // ORDER BY
        if (_order_by.length() > 0) {
            SQL += " ORDER BY " + _order_by;
        }
        // GROUP BY
        if (_group_by.length() > 0) {
            SQL += " GROUP BY " + _group_by;
        }
        // LIMIT
        if (_limit.length() > 0) {
            SQL += " LIMIT " + _limit;
        }
        this.clearQuery();
        return SQL;
    }

    public wActiveRecord clearQuery() {

        _where_in.clear();
        _join.clear();
        _where.clear();
        _like.clear();
        _or_like.clear();
        _select = "*";
        _group_by = "";
        _order_by = "";

        return this;
    }

    /**
     * Connect to database
     */
    private void _connect() {
        if (_db_connection != null) {
            return;
        }
        try {
            _db_connection = DriverManager.getConnection("jdbc:mysql://" + _host + ":" + _port + "/" + _database, _user, _pass);
            _statement = _db_connection.createStatement();
        } catch (SQLException ex) {
            log("Couln't not connect to database", true);
            log(ex.getMessage(), true);
            System.exit(0);
        }
    }

    /**
     * Close active connection
     */
    private void _closeConnection() {
        try {
            if (_db_connection != null) {
                _db_connection.close();
                _db_connection = null;
            }
        } catch (Exception ex) {
        }
    }

    public boolean insert(Class<? extends Serializable> table, List<? extends Serializable> columns, List<? extends Serializable> values) {
        this._table = table.getAnnotation(Table.class).name();

        String SQL = String.format("INSERT INTO %s (%s) VALUES (%s)", _table, getParams(columns, false), getParams(values, true));
        log(SQL);
        try {

            _statement.executeUpdate(SQL);

            log(String.format("Query executed: \"%s\"", SQL));
            return true;

        } catch (SQLException ex) {
            log(ex.getMessage(), true);
            log(SQL, true);

            return false;
        }



    }

    private String getParams(List<? extends Serializable> params, boolean withQuotes) {

        StringBuilder sb = new StringBuilder();

        for (Serializable param : params) {

            if (withQuotes) {

                if (param instanceof String) {
                    sb.append(String.format("\'%s\',", param));
                } else {
                    sb.append(String.format("%s,", param));
                }

            } else {
                sb.append(String.format("%s,", param));
            }


        }


        return sb.substring(0, sb.length() - 1);


    }
}
