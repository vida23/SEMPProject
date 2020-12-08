#ifndef room_h
#define room_h

#include <arduino.h>

class Room {
  private:
    String _name;
    float _coefficient;
    uint8_t _radiation;
  public:
    Room(){_name="";_coefficient=0; _radiation=30;}
    Room(String roomName, float coefficient, uint8_t radiation): _name(roomName),_coefficient(coefficient), _radiation(radiation){}
    uint8_t getRad();
    void setRad(uint8_t radiation);
    float getCoefficient();
    String getName();
    void setRoom(Room room);
};

#endif
