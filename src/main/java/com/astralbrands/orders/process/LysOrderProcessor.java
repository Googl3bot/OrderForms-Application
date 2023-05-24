package com.astralbrands.orders.process;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;
import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class LysOrderProcessor implements BrandOrderForms, AppConstants {

        Logger log = LoggerFactory.getLogger(LysOrderProcessor.class);

        @Autowired
        X3BPCustomerDao x3BPCustomerDao;

        @Value("${lys.id}")
        private String customerId;

        @Value("${lys.site}")
        private String site;

        @Value("${lys.ordertype}")
        private String orderType;

    static Map<String, Integer> colName = new HashMap<>();
    static {
        colName.put(INS_DATE, 0); // Not used
        colName.put(ORDER_DATE, 1);
        colName.put(STATUS, 2); // Not used
        colName.put(PO_NUM, 3);
        colName.put(CUST_ORD_NUM, 4);
        colName.put(BILL_NAME, 5);
        colName.put(BILL_ADD, 6);
        colName.put(BILL_ADD2, 7);
        colName.put(BILL_CITY, 8);
        colName.put(BILL_ST, 9);
        colName.put(BILL_ZIP, 10);
        colName.put(BILL_COUNTRY, 11);
        colName.put(SHIP_NAME, 12);
        colName.put(SHIP_ADD, 13);
        colName.put(SHIP_ADD2, 14);
        colName.put(SHIP_CITY, 15);
        colName.put(SHIP_STATE, 16);
        colName.put(SHIP_ZIP, 17);
        colName.put(SHIP_COUNTRY, 18);
        colName.put(TAX, 19); // Not used
//        colName.put(TAX_CURRENCY, 20); // Not used
        colName.put(VEN_SKU, 21);
        colName.put(DESCRIPTION, 22);
        colName.put(QTY_HUB, 23);
        colName.put(UNIT_COST, 24);
    }


        @Override
        public void process(Exchange exchange, String site, String[] fileNameData) {

            try {
                InputStream inputStream = exchange.getIn().getBody(InputStream.class);
                Workbook workbook = new XSSFWorkbook(inputStream);
                StringBuilder txtFileBuilder = new StringBuilder();
                StringJoiner fileData = new StringJoiner("_");
                fileData.add(fileNameData[0]);
                fileData.add(fileNameData[1]);
                fileData.add(fileNameData[2]);
                String file = fileData.toString();
                String date = currentDate();
                Sheet firstSheet = workbook.getSheetAt(0);
                String txtFileData = populateTxtString(firstSheet,txtFileBuilder);
                System.out.println("Output data is : "+ txtFileData);
                if (txtFileData != null) {
                    exchange.setProperty(INPUT_FILE_NAME, file);
                    exchange.getMessage().setBody(txtFileData);
                    exchange.getMessage().setHeader(Exchange.FILE_NAME, date + "_LYS.txt");
                    exchange.setProperty(IS_DATA_PRESENT, true);
                    exchange.setProperty(LOCAL_ORDER, LOCAL);

                } else {
                    exchange.setProperty(IS_DATA_PRESENT, false);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

        private String populateTxtString(Sheet firstSheet, StringBuilder txtFileBuilder) {

            boolean skipHeader = true;

            String tmpPO = EMPTY_STR;
            System.out.println("String builder is : "+ txtFileBuilder.toString());
            System.out.println("Number of cells are : "+firstSheet);

            for(Row row : firstSheet) {
                ArrayList<Cell> cells = new ArrayList<>();
                Iterator<Cell> cellIterator = row.cellIterator();
                cellIterator.forEachRemaining(cells::add);
                cells.size();
                System.out.println(cells.size());
                if(cells.size()>3) {
                    if(skipHeader) {
                        skipHeader=false;
                    }
                    else if(tmpPO.equals(getValue(cells.get(3)))){
                        txtFileBuilder.append(getOrderLine(row));
                        System.out.println("String builder is : "+ txtFileBuilder.toString());
                        txtFileBuilder.append(NEW_LINE_STR);
                        tmpPO=getValue(cells.get(3));
                        System.out.println("tmpPo number is : "+ tmpPO);
                    }
                    else {
                        System.out.println("row value is "+row.getCell(2));
                        txtFileBuilder.append(getHeader(row));
                        System.out.println("String builder is : "+ txtFileBuilder.toString());
                        txtFileBuilder.append(NEW_LINE_STR);
                        txtFileBuilder.append(getOrderLine(row));
                        System.out.println("String builder is : "+ txtFileBuilder.toString());
                        txtFileBuilder.append(NEW_LINE_STR);
                        tmpPO=getValue(cells.get(3));
                        System.out.println("tmpPo number is : "+ tmpPO);
                    }
                }
            }
            System.out.println("Text file is : "+txtFileBuilder.toString());
            return txtFileBuilder.toString();
        }

        private String getOrderLine(Row row) {

            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            Cell venSku = row.getCell(colName.get(VEN_SKU));
            Cell description = row.getCell(colName.get(DESCRIPTION));
            Cell qty = row.getCell(colName.get(QTY_HUB));
            Cell unitCost = row.getCell(colName.get(UNIT_COST));

            System.out.println("row is : "+cellIterator);
            StringJoiner lineBuilder = new StringJoiner("~");
            System.out.println("Line builder is : "+ lineBuilder.toString());
            lineBuilder.add("L");
            lineBuilder.add(getValue(venSku)); //SKU
            lineBuilder.add(getValue(description)); //Description
//            lineBuilder.add(site); //site
            lineBuilder.add(EA_STR); //Sales Unit
            lineBuilder.add(getValue(qty)); //Quantity
            lineBuilder.add(getValue(unitCost)); //Gross price
            lineBuilder.add(ZERO);
            lineBuilder.add(EMPTY_STR);

            return lineBuilder.toString();
        }

        public String getData(Cell cell) {
            return new DataFormatter().formatCellValue(cell);
        }

        private String getValue(Cell cell) {
            System.out.println("value is : "+cell.toString());
            String value = getData(cell);
            System.out.println("Value is : "+value);
            if(value.toString().equalsIgnoreCase("N/A")) {
                return EMPTY_STR;
            }
            return value.toString();
        }

        private String getHeader(Row row) {

            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            StringJoiner headerBuilder = new StringJoiner("~");
            System.out.println("headerBuilder is : "+ headerBuilder.toString());
            //System.out.println(Header is : "+row);

            Cell poNumber = row.getCell(colName.get(PO_NUM));
            Cell orderDate = row.getCell(colName.get(ORDER_DATE));
            Cell custOrderNum = row.getCell(colName.get(CUST_ORD_NUM));
            Cell billName = row.getCell(colName.get(BILL_NAME));
            Cell billAdd = row.getCell(colName.get(BILL_ADD));
            Cell billAddTwo = row.getCell(colName.get(BILL_ADD2));
            Cell billZip = row.getCell(colName.get(BILL_ZIP));
            Cell billState = row.getCell(colName.get(BILL_ST));
            Cell billCountry = row.getCell(colName.get(BILL_COUNTRY));
            Cell shipName = row.getCell(colName.get(SHIP_NAME));
            Cell shipAdd = row.getCell(colName.get(SHIP_ADD));
            Cell shipAddTwo = row.getCell(colName.get(SHIP_ADD2));
            Cell shipZip = row.getCell(colName.get(SHIP_ZIP));
            Cell shipCity = row.getCell(colName.get(SHIP_CITY));
            Cell shipState = row.getCell(colName.get(SHIP_STATE));
            Cell shipCountry = row.getCell(colName.get(SHIP_COUNTRY));
            Cell tax = row.getCell(colName.get(TAX));



            headerBuilder.add("E");
            headerBuilder.add(site); //Sales Site/SALFCY
            headerBuilder.add(orderType); //Order Type/SOHTYP
            headerBuilder.add(getData(poNumber)); //PO number
            headerBuilder.add(customerId); //BPCORD
            headerBuilder.add(getDate(getValue(orderDate))); //Date
            headerBuilder.add(getValue(custOrderNum)); //Customer order reference
            headerBuilder.add(site); // Shipping site
            headerBuilder.add("USD"); //Currency
            for(int i=0; i<5; i++) {
                headerBuilder.add(EMPTY_STR);
            }
            headerBuilder.add(getValue(billName)); //Bill firstName
            headerBuilder.add(EMPTY_STR); //Bill lastName
            headerBuilder.add(getValue(billCountry)); // Bill city
            headerBuilder.add(getValue(billAdd)); //Bill Add 1
            headerBuilder.add(getValue(billAddTwo)); //Bill Add 2
            headerBuilder.add(getValue(billZip)); //Bill postal code
            headerBuilder.add(getValue(billState)); //Bill state
            headerBuilder.add(getValue(shipName)); //Ship firstname
            headerBuilder.add(EMPTY_STR); //Ship LastName
            headerBuilder.add(getValue(shipCountry)); // Bill city
            headerBuilder.add(getValue(shipAdd)); //Ship Add 1
            headerBuilder.add(getValue(shipAddTwo)); //Ship Add 2
            headerBuilder.add(getValue(shipZip)); //Ship Postal code
            headerBuilder.add(getValue(shipCity)); //Ship city
            headerBuilder.add(getValue(shipState)); //Ship State
            headerBuilder.add(ZERO);
            headerBuilder.add(ZERO);
//            headerBuilder.add(getValue(tax)); // Tax
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add("NET90");
            return headerBuilder.toString();
        }

        private String getDate(String date) {
            System.out.println("Date is : "+date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
            LocalDate ld = LocalDate.parse(date,formatter);
            System.out.println("Date is : "+ld.getMonthValue()+" "+ld.getDayOfMonth()+" "+ld.getYear());
            return ld.getYear()+""+(ld.getMonthValue()<10?("0"+ld.getMonthValue()):ld.getMonthValue())+""+(ld.getDayOfMonth()<10?("0"+ld.getDayOfMonth()):ld.getDayOfMonth());
        }

    private String currentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.now();
//		LocalDate date = LocalDate.parse(today, formatter);
        String td = date.format(formatter);
        return td;
    }


}
