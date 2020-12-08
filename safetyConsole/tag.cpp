#include "tag.h"

void Tag::setId(String id) {
  _id = id;
}

void Tag::setIsCheckedIn(bool isCheckedIn) {
  _isCheckedIn = isCheckedIn;
}

String Tag::getId() {
  return _id;
}

bool Tag::isCheckedIn() {
  return _isCheckedIn;
}
