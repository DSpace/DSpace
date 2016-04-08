package ua.edu.sumdu.essuir.service;

import com.sun.rowset.CachedRowSetImpl;
import org.dspace.core.ConfigurationManager;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.rowset.CachedRowSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service("databaseService")
public class DatabaseService {
    private Statement statement;

    @PostConstruct
    private void init() {
        try {
            Connection connection = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                    ConfigurationManager.getProperty("db.username"),
                    ConfigurationManager.getProperty("db.password"));
            statement = connection.createStatement();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CachedRowSet executeQuery(String query) {
        CachedRowSet resultSet = null;
        try {
            resultSet = new CachedRowSetImpl();
            resultSet.populate(statement.executeQuery(query));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }
}
