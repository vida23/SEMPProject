#include "datapackage.h"

Room DataPackage::getRoom() {
  return _room;
}

Tag DataPackage::getTag() {
  return _tag;
}
Tag* DataPackage::getTagPointer(){
  return &_tag;
}

bool DataPackage::getHazmat(){
  return _hazmat;  
}
void DataPackage::setHazmat(bool hazmat){
  _hazmat = hazmat;
}
char* DataPackage::toBinary() {
  
  char trueChar = 'T';
  char falseChar = 'F';
  char* datapackageBinaryArray = new char [PACKAGE_SIZE];
  
  // Id
  Tag idString = getTag();
  idString.getId().toCharArray(datapackageBinaryArray, PACKAGE_SIZE);

  // IsCheckedIn 
  char isCheckedIn = (getTag().isCheckedIn() ? trueChar : falseChar);

  //Room radiation
  uint8_t radInteger = getRoom().getRad()*getRoom().getCoefficient();
  uint8_t radDecimal = (10 * ((getRoom().getRad()*getRoom().getCoefficient()) - radInteger));
  if(radDecimal == 0){
    radDecimal = 254;
  }
  
  // Hazmat
  char hazmat = (_hazmat ? trueChar : falseChar);

  // Put the rest in the array
  datapackageBinaryArray[11] = isCheckedIn;
  datapackageBinaryArray[12] = radInteger;
  datapackageBinaryArray[13] = radDecimal;
  datapackageBinaryArray[14] = hazmat;
  datapackageBinaryArray[15] = END_DATA;
  return datapackageBinaryArray;
}
