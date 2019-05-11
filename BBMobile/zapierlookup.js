output = { phoneNumber: inputData.phoneNumber, name: "Unknown", SMSMessage: inputData.SMSMessage }

var phoneNumbers = [
  { phoneNumber: "+61401890186", name: "Ric Pruss" },
  { phoneNumber: "+14082198236", name: "Richard McDougal" },
  { phoneNumber: "+13175314204", name: "Dan Wilson" },
  { phoneNumber: "+19177423096", name: "Liam Doyle" },
  { phoneNumber: "+14088380332", name: "Woodson Martin" },
  { phoneNumber: "+16172306603", name: "Andy McAfee" },
  { phoneNumber: "+14158198919", name: "Jonathan Clark" },
  { phoneNumber: "+16504003438", name: "Leslie Fine" },
  { phoneNumber: "+16507977923", name: "Luisa Randon" },
  { phoneNumber: "+16503882006", name: "Joon Yun" },
  { phoneNumber: "+16504003437", name: "Edward Fine" },
  { phoneNumber: "+16503463643", name: "Dana Kornfeld" },
  { phoneNumber: "+14152608476", name: "Margaret Francis" },
  { phoneNumber: "+14156917425", name: "Rick Hartwig" },
  { phoneNumber: "+19178806332", name: "Allan Title" },
  { phoneNumber: "+14156024272", name: "Jos Boumans" },
  { phoneNumber: "+19175123845", name: "Adrian Kunzle" },
  { phoneNumber: "+13868820178", name: "Cleopatra" },
]

var foundName = phoneNumbers.filter((person) => {
  return person.phoneNumber == inputData.phoneNumber;
});

if (foundName[0])
  output.name = foundName[0].name

return output;