#ifndef tag_h
#define tag_h

#include <arduino.h>
#include <MFRC522.h>
#include <MFRC522Extended.h>

class Tag : public MFRC522 {
  private:
    String _id;
    bool _isCheckedIn;
  public:
    Tag() { _id = ""; _isCheckedIn = false;}
    Tag(uint8_t SS_PIN, uint8_t RST_PIN, bool isCheckedIn = false, uint8_t id = 0): MFRC522(SS_PIN, RST_PIN), _id(id), _isCheckedIn(isCheckedIn) {}
    String getId();
    void setId(String id);
    void setIsCheckedIn(bool isCheckedIn);
    bool isCheckedIn();
};

#endif
