#include <SPI.h>
#include <ST7032_asukiaaa.h>
#include <TimeLib.h>
#include <SoftwareSerial.h>
#include <deprecated.h>
#include <require_cpp11.h>
#include "tag.h"
#include "room.h"
#include "datapackage.h"
#include <LiquidCrystal.h>
#include <Bounce2.h>

//Tag
#define USERTAG       "18520910172"
#define RST_PIN       9
#define SS_PIN        10

//Buttons
#define BUTTON_LEFT   A0
#define BUTTON_RIGHT  A1
#define BUTTON_CENTER A4

//Bluetooth
#define RX_PIN        A2
#define TX_PIN        A3
#define BREAKROOM     101
#define BT_BAUD_RATE  9600

//Head States
#define SYSTEM  0
#define USER    1

//System states
#define ALERT   2
#define WRITE   3
#define SEND    4

//Menu states
#define CLOCK_IN           0
#define HAZMAT             1
#define RADIATION          2
#define ROOM               3
#define DEFAULT_MENU       9
#define DEFAULT_MENU_SIZE  4
#define LEAVE_TIME         10

//State Intent
#define CHECK_IN           12
#define CHECK_OUT          13
#define RETURN_TO_MENU     14
#define MENU_GO_RIGHT      15
#define MENU_GO_LEFT       16
#define MENU_ITEM_SELECTED 17
#define ROOM_GO_RIGHT      18
#define ROOM_GO_LEFT       19
#define ROOM_ITEM_SELECTED 20

//Rooms
#define BREAK_ROOM_COEFFICIENT    0.1
#define CONTROL_ROOM_COEFFICIENT  0.5
#define REACTOR_ROOM_COEFFICIENT  1.6
#define DEFAULT_RADIATION         30

Room breakRoom("Break room", BREAK_ROOM_COEFFICIENT, DEFAULT_RADIATION);
Room controlRoom("Control room", CONTROL_ROOM_COEFFICIENT, DEFAULT_RADIATION);
Room reactorRoom("Reactor room", REACTOR_ROOM_COEFFICIENT, DEFAULT_RADIATION);
Room selectedRoom;

//Functions
void menu();
void printDisplay(uint8_t displaySetting, String value = "");
void clearDisplay();
void inputListener(int inputRules);
void inputResponse(int responseRules);
void updateAllButtons();
void controlledRise(Bounce button);
bool scanTag(); //if tag is found, store it and return true.

//tag var.
String tag;
Tag userTag(SS_PIN, RST_PIN);

//Bluetooth
DataPackage userData(userTag, breakRoom, false);
char* dataPackageByteArray;
SoftwareSerial BT (RX_PIN, TX_PIN);

//Buttons
Bounce leftButton = Bounce();
Bounce rightButton = Bounce();
Bounce centerButton = Bounce();

//Global
uint8_t stateHead = USER;
uint8_t stateMenu = CLOCK_IN;
uint8_t stateSystem = WRITE;
bool menuItemSelected = false;
bool tagItemSelected = false; //only if succesfull scan.
LiquidCrystal lcd(7, 6, 5, 4, 3, 2);
time_t time;

void setup() {
  Serial.begin(9600);
  
  // LCD
  lcd.begin(16, 2);

  // Buttons
  leftButton.attach(BUTTON_LEFT, INPUT_PULLUP);
  leftButton.interval(5);
  rightButton.attach(BUTTON_RIGHT, INPUT_PULLUP);
  rightButton.interval(5);
  centerButton.attach(BUTTON_CENTER, INPUT_PULLUP);
  centerButton.interval(5);

  // RFID-TAG
  SPI.begin();        // Init SPI bus
  userTag.PCD_Init(); // Init MFRC522 card
  userTag.PCD_DumpVersionToSerial();

  // Bluetooth
  BT.begin(BT_BAUD_RATE);
  pinMode(RX_PIN, INPUT);
  pinMode(TX_PIN, OUTPUT);

  //userData
  selectedRoom.setRoom(breakRoom);
}
void loop() {
  //For future bluetooth implementation:
  if (BT.available()) {
    //Check alert or time. then tell system to handle
    //printDisplay(ALERT, "Warning!!!");
  }
  
  switch (stateHead) {
    case SYSTEM:
      switch (stateSystem) {
        case WRITE:
          //write package
          dataPackageByteArray = userData.toBinary();
          Serial.write(dataPackageByteArray, 16);
          stateSystem = SEND;
          break;
        case SEND:
          //send package
          if (dataPackageByteArray != NULL) {
            for (int i = 0; i < 16; i++) {
              BT.write(dataPackageByteArray[i]);
            }
          }
          delete dataPackageByteArray;
          stateSystem = WRITE; //for safety, but optimal add safe state that tells system fault.
          stateHead = USER; // give back controll to user.
          break;
      }
    case USER:
      menu();
      break;
  }
}

void menu() {
  
  inputListener(DEFAULT_MENU); //Set inputs to default menue
  if (menuItemSelected != true && tagItemSelected != true) {
    printDisplay(stateMenu);
  }
  
  switch (stateMenu) {
    case CLOCK_IN:
      if (tagItemSelected == false && scanTag()) { //if scan succedes set that the tag is selected.
        tagItemSelected = true;
        time = now();
      }
      if (tagItemSelected) {
        printDisplay(CLOCK_IN);
        inputListener(CLOCK_IN);
      }
      break;
    case HAZMAT:
      if (menuItemSelected) {
        time = now();
        printDisplay(HAZMAT, (userData.getHazmat() ? "ON" : "OFF"));
        inputListener(HAZMAT);
      }
      break;
    case RADIATION:
      if (menuItemSelected) {
        time = now();
        printDisplay(RADIATION, String(userData.getRoom().getRad()));
        inputListener(RADIATION);
      }
      break;
    case ROOM:
      if (menuItemSelected) {
        time = now();
        printDisplay(ROOM, userData.getRoom().getName());
        inputListener(ROOM);
      }
      break;
  }
}

void inputListener(int inputRules) {
  time_t timeElapsed = now();
  if (inputRules != DEFAULT_MENU && timeElapsed - time >= LEAVE_TIME) {
    time = 0;
    lcd.clear();
    menuItemSelected = false;
    tagItemSelected = false;
    return;
  }
  updateAllButtons();
  
  switch (inputRules) {
    case CLOCK_IN:
      if (leftButton.fell()) { //check in
        inputResponse(CHECK_IN);
        controlledRise(leftButton);
      }
      else if (rightButton.fell()) { //check out
        inputResponse(CHECK_OUT);
        controlledRise(rightButton);
      }
      else if (centerButton.fell()) { // do neither, return to menu.
        inputResponse(RETURN_TO_MENU);
        controlledRise(centerButton);
      }
      break;
      
    case HAZMAT:
      if (rightButton.fell()) {
        userData.setHazmat(!userData.getHazmat());
        controlledRise(rightButton);
      }
      if (centerButton.fell()) {
        inputResponse(RETURN_TO_MENU);
        controlledRise(centerButton);
      }
      break;
      
    case RADIATION:
      //if potentiometer moved then potentiometerfunction and display
      if (centerButton.fell()) {
        inputResponse(RETURN_TO_MENU);
        controlledRise(centerButton);
      }
      break;
      
    case ROOM:
      if (leftButton.fell()) {
        inputResponse(ROOM_GO_LEFT);
        controlledRise(leftButton);
      }
      else if (rightButton.fell()) {
        inputResponse(ROOM_GO_RIGHT);
        controlledRise(rightButton);
      }
      else if (centerButton.fell()) {
        inputResponse(ROOM_ITEM_SELECTED);
        controlledRise(centerButton);
      }
      break;
      
    case DEFAULT_MENU:
      if (leftButton.fell()) { //check in
        inputResponse(MENU_GO_LEFT);
        controlledRise(leftButton);
      }
      else if (rightButton.fell()) { //check out
        inputResponse(MENU_GO_RIGHT);
        controlledRise(rightButton);
      }
      else if (centerButton.fell()) {
        inputResponse(MENU_ITEM_SELECTED);
        controlledRise(centerButton);
      }
      break;
  }
}

void inputResponse(int response) {
  switch (response) {
    case CHECK_IN:
      tagItemSelected = false; //after this stateMenu machine will wait for a tag.
      
      if (userData.getTag().isCheckedIn() == true) {
        printDisplay(CLOCK_IN,"User already clocked in");
        break;
      }
      else { //not checked in, checking person in.
        printDisplay(CLOCK_IN,"User clocked in");
        userData.getTagPointer()->setIsCheckedIn(true);
        
        //tell system to write which will also leads to send package.
        stateHead = SYSTEM;
        stateSystem = WRITE;
      }
      break;
      
    case CHECK_OUT:
      tagItemSelected = false; //after this stateMenu machine will wait for a tag.
      if (userData.getTag().isCheckedIn() == false) {
        printDisplay(CLOCK_IN,"User already clocked out");
        break;
      }
      else {
        printDisplay(CLOCK_IN, "User clocked out");
        userData.getTagPointer()->setIsCheckedIn(false);
        
        //tell system to write which will also leads to send package.
        stateHead = SYSTEM;
        stateSystem = WRITE;
      }
      break;
      
    case RETURN_TO_MENU:
      lcd.clear();
      tagItemSelected = false;
      menuItemSelected = false;
      break;
    case MENU_GO_RIGHT:
      stateMenu = (stateMenu + 1) % DEFAULT_MENU_SIZE;
      break;
    case MENU_GO_LEFT:
      stateMenu = (((stateMenu - 1) % DEFAULT_MENU_SIZE) + DEFAULT_MENU_SIZE) % DEFAULT_MENU_SIZE;
      break;
    case MENU_ITEM_SELECTED:
      if(stateMenu != 0){
        lcd.clear();
        menuItemSelected = true;
      }
      else{
        printDisplay(CLOCK_IN, "    Use Tag");
      }
      break;
    case ROOM_GO_RIGHT:
      //Future implementation
      break;
    case ROOM_GO_LEFT:
      //Future implementation
      break;
    case ROOM_ITEM_SELECTED:
      //Future implementation
      menuItemSelected = false;
      break;
  }
}

void updateAllButtons() {
  leftButton.update();
  rightButton.update();
  centerButton.update();
}

void controlledRise(Bounce button) {
  while (!button.rose()) {
    button.update();
  }
}

bool scanTag() {
  if ( ! userTag.PICC_IsNewCardPresent()) {
    return false;
  }
  
  // Select one of the cards
  if ( ! userTag.PICC_ReadCardSerial()) {
    printDisplay(CLOCK_IN,"Bad read, try again");
    return false;
  }
  
  if (userTag.uid.size == 0) {
    printDisplay(CLOCK_IN,"Bad card...");
  }
  else {
    //Putting the uid into the tag._id 
    userData.getTagPointer()->setId("");
    for (int i = 0; i < userTag.uid.size; i++) {
      userData.getTagPointer()->setId(String(userData.getTag().getId() + userTag.uid.uidByte[i]));
    };
  };
  
  if (userData.getTag().getId().compareTo(USERTAG) == 0) {
    printDisplay(CLOCK_IN,"User identified");
    
    // disengage with the card.
    userTag.PICC_HaltA();
    return true;
  }
  else {
    printDisplay(CLOCK_IN,"Unauthorized personal");
    
    // disengage with the card.
    userTag.PICC_HaltA();
    return false;
  }
  return false; //for safety
}

void printDisplay(uint8_t displaySetting, String value) {
  
  switch (displaySetting) {
    case CLOCK_IN:
      if (tagItemSelected) {
        lcd.setCursor(2, 0);
        lcd.print("Clock in/out");
        lcd.setCursor(0, 1);
        lcd.print("< In       out >");
        lcd.setCursor(0, 0);
      }
      else {
        if (value == "") {
          lcd.setCursor(6, 0);
          lcd.print("Menu");
          lcd.setCursor(1, 1);
          lcd.print("<  Clock in  >");
        }
        else {
          lcd.setCursor(0,0);
          lcd.clear();
          lcd.setCursor(0,1);
          lcd.clear();
          displayBigString(value);
        }
        lcd.setCursor(0, 0);
      }
      break;
      
    case HAZMAT:
      if (menuItemSelected) {
        lcd.setCursor(5, 0);
        lcd.print("Hazmat");
        lcd.setCursor(0, 1);
        
        if (value == "ON") {
          lcd.print(" Suit on | OFF >");
        }
        else if (value == "OFF") {
          lcd.print(" Suit off | ON >");
        }
        lcd.setCursor(0, 0);
      }
      else {
        lcd.setCursor(6, 0);
        lcd.print("Menu");
        lcd.setCursor(1, 1);
        lcd.print("<   Hazmat   >");
        lcd.setCursor(0, 0);
      }
      break;
      
    case RADIATION:
      if (menuItemSelected) {
        lcd.setCursor(3, 0);
        lcd.print("Radiation");
        lcd.setCursor(5, 1);
        lcd.print(value);
        lcd.setCursor(0, 0);
      }
      else {
        lcd.setCursor(6, 0);
        lcd.print("Menu");
        lcd.setCursor(1, 1);
        lcd.print("< Radiation  >");
        lcd.setCursor(0, 0);
      }
      break;
      
    case ROOM:
      if (menuItemSelected) {
        lcd.setCursor(6, 0);
        lcd.print("Room");
        lcd.setCursor(1, 1);
        lcd.print("<");
        lcd.setCursor(3, 1);
        lcd.print(value);
        lcd.setCursor(14, 1);
        lcd.print(">");
        lcd.setCursor(0, 0);
      }
      else {
        lcd.setCursor(6, 0);
        lcd.print("Menu");
        lcd.setCursor(1, 1);
        lcd.print("<    Room    >");
        lcd.setCursor(0, 0);
      }
      break;
      //for future implementation
      //case ALERT:
      //break;
    default:
      break;
  }

}

void clearDisplay() {
  delay(2000);
  lcd.clear();
}

void displayBigString(String string) {
  lcd.print(string);
  if (string.length() > 16) {
    delay(800);
    for (int i = 0; i < (string.length() - 16); i++) {
      lcd.scrollDisplayLeft();
      delay(400);
    }
  }
  clearDisplay();
}
