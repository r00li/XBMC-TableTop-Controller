/*
 * XBMC TableTop Controller version 1.0
 * http://www.r00li.com
 * Copyright 2013 Andrej Rolih
 * Licensed under GPLv3 - see LICENSE.txt
 * 
 * Uses standard Arduino libraries - tested with Arduino 0018
 * Look at the PCB file for hardware information.
 */



// include the library code:
#include <LiquidCrystal.h>

// initialize the library with the numbers of the interface pins
LiquidCrystal lcd(12, 11, 5, 4, 3, 2);

byte c0[8] = {
  B00000,
  B00000,
  B00000,
  B00000,
  B00000,
  B00000,
  B00000,
};

byte c1[8] = {
  B10000,
  B10000,
  B10000,
  B10000,
  B10000,
  B10000,
  B10000,
};

byte c2[8] = {
  B11000,
  B11000,
  B11000,
  B11000,
  B11000,
  B11000,
  B11000,
};

byte c3[8] = {
  B11100,
  B11100,
  B11100,
  B11100,
  B11100,
  B11100,
  B11100,
};

byte c4[8] = {
  B11110,
  B11110,
  B11110,
  B11110,
  B11110,
  B11110,
  B11110,
};

byte c5[8] = {
  B11111,
  B11111,
  B11111,
  B11111,
  B11111,
  B11111,
  B11111,
};

byte play[8] = {
  B10000,
  B11000,
  B11100,
  B11110,
  B11100,
  B11000,
  B10000,
};

byte pause[8] = {
  B11011,
  B11011,
  B11011,
  B11011,
  B11011,
  B11011,
  B11011,
};


char buff[26];
byte buff_pointer = 0;

char miniBuff[4];

long lastmessage = 0;



void setup() {
  // set up the LCD's number of rows and columns: 
  lcd.begin(24, 2);
  
  lcd.createChar(7, play);
  lcd.createChar(0, c0);
  lcd.createChar(1, c1);
  lcd.createChar(2, c2);
  lcd.createChar(3, c3);
  lcd.createChar(4, c4);
  lcd.createChar(5, c5);
  lcd.createChar(6, pause);

  pinMode(13,OUTPUT);
  digitalWrite(13, LOW);
  
  pinMode(6, INPUT);
  digitalWrite(6, HIGH);
  pinMode(7, INPUT);
  digitalWrite(7, HIGH);
  pinMode(8, INPUT);
  digitalWrite(8, HIGH);
  pinMode(9, INPUT);
  digitalWrite(9, HIGH);
  pinMode(10, INPUT);
  digitalWrite(10, HIGH);
  pinMode(14, INPUT);
  digitalWrite(14, HIGH);
  pinMode(15, INPUT);
  digitalWrite(15, HIGH);
  pinMode(16, INPUT);
  digitalWrite(16, HIGH);
  pinMode(17, INPUT);
  digitalWrite(17, HIGH);
  pinMode(18, INPUT);
  digitalWrite(18, HIGH);
  pinMode(19, INPUT);
  digitalWrite(19, HIGH);
  
  Serial.begin(9600);
  
  lcd.clear();
  lcd.print("XBMC controller v1.0");
  lcd.setCursor(0,1);
  lcd.print("Not plugged in");
}

void messageReceived() {
  if (buff_pointer >= 1) {
    
    if (buff[0] == 'L') {
      digitalWrite(13, HIGH);
      lcd.setCursor(0, 0);
      int pos = 1;
      while(pos < buff_pointer) {
        lcd.print(buff[pos]);
        pos++;
      }
    }
    else if (buff[0] == 'R') {
      lcd.setCursor(16, 0);
      int pos = 1;
      while(pos < buff_pointer) {
        lcd.print(buff[pos]);
        pos++;
      }
    }
    else if (buff[0] == 'P') {
      
      miniBuff[0] = buff[1];
      miniBuff[1] = buff[2];
      miniBuff[2] = '\0';
      
      int percentage = atoi(miniBuff);
      int prctg = map(percentage, 0, 99, 0, 40);
      
      lcd.setCursor(8,0);
      lcd.print("        ");
      lcd.setCursor(8,0);
      for (int i = 0; i < prctg/5; i++)
        lcd.write(byte(5));
        
      lcd.write(byte(prctg%5));
    }
    else if (buff[0] == 'S') {
      digitalWrite(13, HIGH);
      
      lcd.setCursor(0,1);
      if (buff[1] == '0')
        lcd.write(byte(6));
      else
        lcd.write(byte(7));
        
      int pos = 2;
      while(pos < buff_pointer) {
        lcd.print(buff[pos]);
        pos++;
      }
    }
    else if (buff[0] == 'B') {
      digitalWrite(13, LOW);
      lcd.clear();
      int pos = 1;
      lcd.print("      XBMC OFFLINE");
      lcd.setCursor(0,1);
      while(pos < buff_pointer) {
        lcd.print(buff[pos]);
        pos++;
      }
    }
  }
}

void loop() {
  
    while (Serial.available() > 0) {
    buff[buff_pointer] = Serial.read();
    
    if (buff[buff_pointer] == '\0') {
      lastmessage = millis();
      messageReceived();
      buff_pointer=0;
    }
    else {
      buff_pointer++;
    }
  }
  
  if (digitalRead(6) == LOW) {
    Serial.print("B3"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(7) == LOW) {
    Serial.print("B8"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(8) == LOW) {
    Serial.print("B11"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(9) == LOW) {
    Serial.print("B2"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(10) == LOW) {
    Serial.print("B5"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(14) == LOW) {
    Serial.print("B9"); //gumb pritisnjen
    Serial.print('\0');
    delay(250);
  }
  if (digitalRead(15) == LOW) {
    Serial.print("B10"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(16) == LOW) {
    Serial.print("B7"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(17) == LOW) {
    Serial.print("B6"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(18) == LOW) {
    Serial.print("B4"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }
  if (digitalRead(19) == LOW) {
    Serial.print("B1"); //gumb pritisnjen
    Serial.print('\0');
    delay(270);
  }

  if (millis() - lastmessage > 30000) {
    digitalWrite(13, LOW);
    lcd.setCursor(0,0);
    lcd.print("XBMC controller v1.0    ");
    lcd.setCursor(0,1);
    lcd.print("Not plugged in          ");
  }
  
  // set the cursor to column 0, line 1
  // (note: line 1 is the second row, since counting begins with 0):
  // print the number of seconds since reset:

}

