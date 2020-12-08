#ifndef datapackage_h
#define datapackage_h

#include <arduino.h>
#include <TimeLib.h>
#include "tag.h"
#include "room.h"

#define PACKAGE_SIZE 16
#define END_DATA     255

class DataPackage {
  private:
    Tag _tag;
    Room _room;
    bool _hazmat;
    time_t _time;
  public:
    
    DataPackage::DataPackage(Tag tag, Room room, bool hazmat, time_t timeData = 0) {
      _tag = tag;
      _room = room;
      _hazmat = hazmat;
      _time = timeData;
    }
    Tag getTag();
    Tag* getTagPointer();
    bool getHazmat();
    void setHazmat(bool hazmat);
    Room getRoom();
    char* toBinary();

    //Future implementation:
    //DataPackage convertFromBinary(byte* byteArray);
    
};

#endif
