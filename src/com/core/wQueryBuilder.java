package com.core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class wQueryBuilder {
	
	protected static Connection _db_connection = null;
	protected Statement _statement = null;
	protected ResultSet _resultset = null;
	protected Map<String, String> _join = new HashMap<String, String>();
	protected Map<String, String> _where = new HashMap<String, String>();
	protected Map<String, String[]> _where_in = new HashMap<String, String[]>();
	protected Map<String, String> _like = new HashMap<String, String>();
	protected Map<String, String> _or_like = new HashMap<String, String>();
    
	protected String // Active record
            _db_prefix = "",
            _select = "*",
            _group_by = "",
            _order_by = "",
            _limit = "",
            // Database Configuration
            _host = "localhost",
            _port = "3306",
            _database = null,
            _user = "root",
            _pass = "";
    
	protected String _table;
	
	
}
