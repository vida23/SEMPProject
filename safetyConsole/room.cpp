#include "room.h"

uint8_t Room::getRad() {
  return _radiation;
}

void Room::setRad(uint8_t radiation) {
  _radiation = radiation;
}

float Room::getCoefficient(){
  return _coefficient;  
}

String Room::getName(){
  return _name;
}

void Room::setRoom(Room room){
  _name = room.getName();
  _coefficient = room.getCoefficient();
  _radiation = room.getRad();
}
