package com.astralbrands.orders.constants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/*
	Interface for a set of permanent Strings
	Initialized for
 */
public interface AppConstants {


	public final static String TILDE = "~";
	public final static String CHAR_E = "E";
	public final static String EMPTY_STR = "";
	public final static String ZERO = "0";
	public final static String NEW_LINE_STR = "\r\n";
	public final static String CHAR_L = "L";
	public final static String EA_STR = "EA";
	public final static String QUANTITY = "QTY";
	public final static String DECIMAL_ZERO = ".0";
	public final static String TAB_SPACE = "\t";
	public final static String UNDERSCORE = "_";
	public final static String DOT = ".";
	public final static String COMMA = ",";
	public final static String SEMI_COMMA = ";";
	public final static String IFILE = "iFile.txt";
	public final static String US_STR = "US";
	public final static String US_CURR = "USD";
	public final static String CA_CURR = "CAD";
	public final static String DOT_CSV = ".csv";
	public final static String DOT_TXT = ".txt";
	public final static String SITE_NAME = "SITE_NAME";
	public final static String CSV_DATA = "CSV_DATA";
	public final static String IS_DATA_PRESENT = "isDataPresent";
	public final static String INPUT_FILE_NAME = "INPUT_FILE_NAME";
	public final static String SITE = "SITE"; // FOR INTERNATIONAL ORDER FORMS
	public final static String SITE_TWO = "SITE_TWO"; // FOR LOCAL US ORDER FORMS
	public final static String CUST_NUMBER = "CUSTOMER_NUMBER"; // FOR INTERNATIONAL ORDER FORMS
	public final static String CUST_NUM = "CUSTOMER_NUMBER"; // FOR LOCAL US ORDER FORMS

	public final static String ALOETTE = "ALOETTE";
	public final static String COS = "COSMEDIX";
	public final static String PURCOS = "PUR_COSMETICS";
	public final static String HUB = "COMMERCE_HUB";
	public final static String BRAND = "BRAND";
	public final static String NYKKA = "NYKKA";
	public final static String LOCAL = "LOCAL";
	public final static String LOCAL_ORDER = "LOCAL_ORDER"; // FOR DIFFERENTIATING LOCAL ORDER FORMS

	public final static String FILE_NAME_ARRAY = "FILE_NAME_ARRAY";




	//---------------Mock Order Forms Column Names ------------------


	public static final String ITEM = "ITEM#";
	public static final String QTY = "Qty";
	public static final String TOTAL_COST = "Total.Cost";
	public static final String ORDER_NUM = "Order #";
	public static final String ITEM_DESC = "ItemDesc";
	public static final String SIZE = "Size";
	public static final String ITEM_COST = "Item/Cost";
	public static final String INT_COST = "Int.Cost";

	public static final String ITEM_NAME = "ItemName";
	public static final String DIST_COST = "Dist.Cost";
	public static final String NAME = "Name";
	public static final String STREET = "Street";
	public static final String CITY = "City";
	public static final String SHIPTO = "ShipTo";


	//---------------Aloutte Order Forms Column names----------------


	public static final String PROD_DESC = "Product Description";
	public static final String MIN = "Min";
	public static final String STOCK_NUM = "Stock #";
	public static final String WHL = "Whl";
	public static final String QTY_A = "Qty";
	public static final String EXT_COST = "EXT Cost";
	public static final String REG_PROD = "Regulated Product"; // Not used


	// ------------------CommerceHub Order Form Column Names----------------------


	public static final String INS_DATE = "Insert Date";
	public static final String ORDER_DATE = "Order Date";
	public static final String STATUS = "Status"; // Not used
	public static final String PO_NUM = "PO Number";
	public static final String CUST_ORD_NUM = "Customer Oder Number";
	public static final String BILL_NAME = "BillTo Name";
	public static final String BILL_ADD = "BillTo Address1";
	public static final String BILL_ADD2 = "BillTo Address2";
	public static final String BILL_CITY = "BillTo City";
	public static final String BILL_ST = "BillTo State";
	public static final String BILL_ZIP = "BillTo Postal Code";
	public static final String BILL_COUNTRY = "BillTo Country";
	public static final String SHIP_NAME = "ShipTo Name";
	public static final String SHIP_ADD = "ShipTo Address1";
	public static final String SHIP_ADD2 = "ShipTo Address2";
	public static final String SHIP_CITY = "ShipTo City";
	public static final String SHIP_STATE = "ShipTo State";
	public static final String SHIP_ZIP = "ShipTo Postal Code";
	public static final String SHIP_COUNTRY = "ShipTo Country";
	public static final String TAX = "Tax";
	public static final String TAX_CURRENCY = "Tax Currency"; // Not used
	public static final String VEN_SKU = "Vendor SKU";
	public static final String DESCRIPTION = "Description";
	public static final String QTY_HUB = "Quantity";
	public static final String UNIT_COST = "Unit Cost";


	//---------------Cosmedix Order Form Column Names-----------------


	public static final String SQID = "SQID";
	public static final String PRODUCTDEC = "PRODUCTDEC";
	public static final String QTY_COS = "QUANTITY";
	public static final String PRICE = "PRICE";
	public static final String Site = "COSMEDIX";


	//-------------------YNSOHTempProceess----------------------

	public static final String ORDER_NO = "Order No";
	public static final String WEB_ORD_NO = "Web Order No";
	public static final String ORDER_TYPE = "Order Type";
	public static final String SELLER_ID = "SellerId";
	public static final String ISBN_UPC = "ISBN/UPC";
	public static final String ORDER_DATE_1 = "Order Date";
	public static final String SELLER = "Seller";
	public static final String SELLER_MOBILE = "Seller Mobile No";
	public static final String CUST_NAME = "Customer Name";
	public static final String SKU = "SKU Code";
	public static final String SKU_NAME = "SKU Name";
	public static final String LINE_AMT = "Line Amount";
	public static final String QTY_EXPORT = "Qty";
	public static final String BUYER_CITY = "Buyer City";
	public static final String BUYER_STATE = "Buyer State";

	//***************----40BOXES_EXPORT_FILES----****************
	public static final String SAL_FCY = "SALFCY";
	public static final String SOH_TYP = "SOHTYP";
	public static final String ORD_NUM = "ORDNUM";
	public static final String SOLD_TO = "SOLDTO";
	public static final String ORD_DAT = "ORDDAT";
	public static final String CUST_ORDREF = "CUSORDREF";
	public static final String SITE_2 = "STOFCY";
	public static final String CUR = "CUR";
	public static final String BPI_NAM_0 = "BPINAM_0";
	public static final String BPI_ADD1 = "BPIADDLIG_1";
	public static final String BPI_ADD2 = "BPIADDLIG_2";
	public static final String BPI_ZIP = "BPIPOSCOD";
	public static final String BPI_CTY = "BPICTY";
	public static final String BPI_STAT = "BPISTAT";
	public static final String BPI_NAM0 = "BPINAM_0";
	public static final String BPD_NAM0 = "BPDNAM_0";
	public static final String BPD_ADD_1 = "BPDADDLIG_1";
	public static final String BPD_ADD_3 = "BPDADDLIG_3";
	public static final String BPD_ZIP = "BPDPOSCOD";
	public static final String BPD_CITY = "BPDCITY";
	public static final String BPD_STAT = "BPDSAT";
	public static final String INV_AMT = "INVDTAAMT_0";
	public static final String INV_AMT_1 = "INVDTAAMT_1";
	public static final String SHIP_FIRST = "MDL";
	public static final String SHIP_SECOND = "BPTNUM";
	public static final String ITM_REF = "ITMREF_0";
	public static final String ITM_DESC = "ITMDES_0";
	public static final String QTY_0 = "QTY_0";
	public static final String NET_PRICE = "NETPRI_0";
	public static final String BOXES = "boxes";

	//***************----40_BOXES_PUR_EBAY_FILES----*****************

	public static final String SALFCY = "SALFCY";
	public static final String SOHTYP = "SOHTYP";
	public static final String SOHNUM = "SOHNUM";
	public static final String SOLDTO = "SOLDTO";
	public static final String ORDDAT = "ORDDAT";
	public static final String CUSORDREF = "CUSORDREF";
	public static final String CUR_PEBAY = "CUR";
	public static final String BPINAM_0 = "BPINAM_0";
	public static final String BPIADD1 = "BPIADDLIG_1";
	public static final String BPIADD2 = "BPIADDLIG_2";
	public static final String BPIZIP = "BPIPOSCOD";
	public static final String BPICTY = "BPICTY";
	public static final String BPISTAT = "BPISTAT";
	public static final String BPINAM0 = "BPINAM_0";
	public static final String BPDNAM0 = "BPDNAM_0";
	public static final String BPDADD_1 = "BPDADDLIG_1";
	public static final String BPDADD_3 = "BPDADDLIG_3";
	public static final String BPDZIP = "BPDPOSCOD";
	public static final String BPDCITY = "BPDCITY";
	public static final String BPDSTAT = "BPDSAT";
	public static final String INVAMT = "INVDTAAMT_0";
	public static final String INVAMT_1 = "INVDTAAMT_1";
	public static final String INVAMT_2 = "INVDTAAMT_2";
	public static final String SHIPFIRST = "MDL";
	public static final String SHIPSECOND = "BPTNUM";
	public static final String ITMREF = "ITMREF_0";
	public static final String ITMDESC = "ITMDES_0";
	public static final String UOM = "UOM";
	public static final String QTY0 = "QTY_0";
	public static final String NETPRICE = "NETPRI_0";


	//***************----PCA_Butter_London_FILES----******************
	public static final String 	PCA_STATUS = "Status";
	public static final String  CATEGORY = "Category";
	public static final String 	Vendor_ID = "Vendor ID";
	public static final String  UPC = "UPC";
	public static final String 	PRODUCT = "Product";
	public static final String  SHADE_NAME = "Shade Name";
	public static final String 	FILL_WEIGHT = "Fill Weight";
	public static final String  US_SRP = "US SRP";
	public static final String 	INT_SRP = "INT SRP";
	public static final String  DISTRIBUTED_DISCOUNT_PERCENTAGE = "Distributor Discount %";
	public static final String 	PCA_DIST_COST = "Dist Cost";
	public static final String  PCA_QTY = "QTY";
	public static final String  EXTENDED_PRICE = "Extended Price";
	//******************----END-OF-PCA-BUTTER-MAPPINGS----**********************


	//***************----BRAND_ORDERS_EXPORT_FILES----****************

	//------------------BUTTER LONDON FORM COLUMN NAMES----------------------
	public static final String B_STATUS = "Status";
	public static final String B_CATEGORY = "Category";
	public static final String B_VENDOR_ID = "Vendor ID";
	public static final String B_UPC = "UPC";
	public static final String B_PRODUCT_DESCR = "Product";
	public static final String B_SHADE_NAME = "Shade Name";
	public static final String B_EU = "EU Registered";
	public static final String B_WEIGHT = "Fill Weight";
	public static final String B_TEST_SKU = "TESTER ID";
	public static final String B_TEST_UPC = "TESTER UPC";
	public static final String B_TEST_COST = "COST";
	public static final String B_TEST_QTY = "Tester QTY";
	public static final String B_TEST_PRICE = "Extended Price";

	//*****************--USED IN EACH FORM--*************************

	public static final String PURCH_ORD_NUM = "PO #:";
	public static final String FORM_DATE = "DATE:";
	public static final String FORM_BRAND = "Brand:";
	public static final String ASTRAL_CUST_NUM = "ASTRAL Customer Number:";
	public static final String BO_CUST_NAME = "CUSTOMER NAME:";
	public static final String SHIP_VIA = "SHIP VIA:";
	public static final String BO_TERMS = "TERMS:";
	public static final String BO_STATUS = "STATUS";
	public static final String MAIN_CAT = "Main Category";
	public static final String SUB_CAT = "Sub-Category";

	//-----------------PUR FORM COLUMN NAMES----------------------------
	public static final String RETAIL_ITEM_NUM = "Retail Item #";
	public static final String RETAIL_UPC = "Retail UPC";
	public static final String EU_RETAIL_NUM = "EU Specific RETAIL #";
	public static final String EU_UPC_NUM = "EU Specific UPC NUMBER";
	public static final String EU_REGISTERED = "EU registered";
	public static final String EU = "EU";
	public static final String BO_PRODUCT_DESCRIPTION = "Product Description";
	public static final String C_F_S = "C/F/S";
	public static final String SHADE_NUM = "Shade Number";
	public static final String BO_PRICE = "U.S. MSRP";
	public static final String INT_SHIP_PRICE = "Int' SRP";
	public static final String DIST_DISCOUNT = "Distributor Discount %";
	public static final String DIST_PRICE = "Distributor Cost";
	public static final String BO_QTY_ORDERED = "Quantity Ordered";
	public static final String EXT_PRICE = "Extended Price";

	//----------------TEST & GROSS PRICE SECTIONS-----------------\\
	public static final String TESTER_SKU = "Tester SKU";
	public static final String TESTER_UPC = "Tester UPC";
	public static final String TESTER_COST = "Tester Cost";
	public static final String TESTER_QTY = "Tester Quantity Ordered";
	public static final String BRANDS = "BRANDS";

	public static final String GRAND_TOTAL = "GRAND TOTAL FOR ORDER";
	public static final String TOTAL_PRICE = "GROSS PRICE";
	public static final String TOTAL_LINES = "TOTAL LINES";
	//---------------CORE KIT SECTION---------------------\\
	public static final String CORE_KIT = "CORE KIT";
	public static final String UPC_NUMBER = "UPC NUMBER";
//	public static final String UPC_NUMBER = "UPC NUMBER";



	//-------------------COS ORDER FORM COLUMN NAMES-------------------------------

	public static final String C_ITEM_NUM = "Item #";
	public static final String C_UPC = "UPC #";
	public static final String C_PRODUCT = "Product";
	public static final String C_PROD_DESCR = "Short Description";
	public static final String C_EU = "EU Registered";
	public static final String C_PROD_SIZE = "Size";
	public static final String C_DIST_PRICE = "Distributor Price";
	public static final String C_QTY = "Quantity of Units Ordered";

	public static final String C_TEST_SKU = "Tester SKU";
	public static final String C_TEST_UPC = "Tester UPC";
	public static final String C_TEST_COST = "Tester Cost";
	public static final String C_TEST_QTY = "Tester Quantity Ordered";


	public static final String B_DIST_COST = "Dist Cost";
	public static final String B_QTY = "QTY";









	// ***************--QUERIES TO THE SQL SERVER TO GET BASE PRICE--*********************\\
	public static final String GET_ITM_REF = "ITMREF_0";






}


