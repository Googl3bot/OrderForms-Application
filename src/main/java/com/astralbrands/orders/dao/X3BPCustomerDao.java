package com.astralbrands.orders.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//import javax.activation.DataSource;
//import javax.sql.DataSource;
import javax.sql.DataSource;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
/*
	Builds a connection to the x3 database
	To retrieve payment terms for a specific
    order using the	Customer ID to associate with
    the corresponding order
 */
@Repository
public class X3BPCustomerDao {


	// GET THE 'ITMREF_0' FROM UPC CODE
	@Value("${db.query}")
	public static String query1;
	// QUERY TO GET BASE PRICE FOR PRODUCT LINES
	@Value("${db.query1}")
	public String queryPrice;
	// GET THE BASE PRICE FOR THE PRODUCT
//	@Value("${db.query2}")
//	public static String paymentTerms;
	@Value("${db.query.product}")
	public String dbQueryTwo;
	private String netId = "NET30";
	private double productPrice;
	private String productDescription;
	@Autowired
	@Qualifier("x3DataSource")
	DataSource x3DataSource;

	@Autowired
	@Qualifier("x3DataSource")
	DataSource x3DB;

	private Connection connection;
	private Connection connectionTwo;
	private Connection connectTwo;

	public String getPaymentTerms(String customerId) {
		System.out.println("Customer id is: " + customerId);
		if (customerId == null) {
			System.out.println();
			return netId;
		}
		//Database object shouldn't be null
		if (x3DataSource != null) {
			try {
				//Establishes a connection with the database
				if (connection == null) {
					connection = x3DataSource.getConnection();
					connection.setAutoCommit(true);
				}
				try (Statement statement = connection.createStatement();) {
					String query = "select PTE_0 from PROD.BPCUSTOMER where BPCNUM_0 = '" + customerId + "'";
					ResultSet paymentTerm = statement.executeQuery(query);
					while (paymentTerm.next()) {
						netId = paymentTerm.getString("PTE_0");
					}
					System.out.println("Payment term for " + customerId + "is: " + netId);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println(" Exception " + e.getMessage());
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
				System.err.println(ex.getMessage());
			}

		} else {
			System.err.println("X3 data source is null");
		}

		return netId;
	}

	public String getBasePrice(String sku) {
		String query = queryPrice + "'" + sku + "'";
		String baseprice = "";
		if (x3DB != null) {
			try {
				if (connectionTwo == null) {
					connectionTwo = x3DB.getConnection();
				}
				Statement statement = connectionTwo.createStatement();
				ResultSet rs = statement.executeQuery(query);
				baseprice = rs.toString();
//				if(baseprice.contains(".")) {
//					baseprice = baseprice.substring(0, baseprice.indexOf("."));
//				} else {
//					return baseprice;
//				}
				statement.close();
			} catch (SQLException bE) {
				String message = "ERROR - Couldn't find the 'Base Price' with - " + query;
				System.err.println(message);
				bE.printStackTrace();
			}
		} else {
			System.err.println("X3 data source is null");
		}
		return baseprice;
	}

	public String getItemDescription(String upc) {
		System.out.println("Product is: " + upc);
		if (upc == null) {
			System.out.println();
			return productDescription;
		}
		//Database object shouldn't be null
		if (x3DB != null) {
			try {
				//Establishes a connection with the database
				if (connectTwo == null) {
					connectTwo = x3DB.getConnection();
					connectTwo.setAutoCommit(true);
				}
				try (Statement statement = connectTwo.createStatement();) {
					String query = dbQueryTwo + "'" + upc + "';";
					ResultSet paymentTerm = statement.executeQuery(query);
					while (paymentTerm.next()) {
						productDescription = paymentTerm.getString("ITMDES1_0");
					}
					System.out.println("Product Description for " + upc + "is: " + productDescription);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println(" Exception " + e.getMessage());
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
				System.err.println(ex.getMessage());
			}

		} else {
			System.err.println("X3 data source is null");
		}

		return productDescription;
	}

	public String getDeliveryNumber(String orderNumber) {

		String deliveryNumber = null;
		if (x3DataSource != null) {
			try {
				if (connection == null) {
					connection = x3DataSource.getConnection();
					connection.setAutoCommit(true);
				}
				try (Statement statement = connection.createStatement();) {
					String query = "select SDHNUM_0 from PROD.SDELIVERY where SOHNUM_0='"+orderNumber+"';";
					//query.replace("#orderNumber",orderNumber);
					ResultSet paymentTerm = statement.executeQuery(query);
					while (paymentTerm.next()) {
						deliveryNumber = paymentTerm.getString("SDHNUM_0");
					}
				}  catch (Exception e) {
					e.printStackTrace();
					System.err.println(" Exception " + e.getMessage());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(" Exception " + e.getMessage());
			}
		}
		else {
			System.err.println("Bit boot data source is null");
		}
		return deliveryNumber;
	}


}
