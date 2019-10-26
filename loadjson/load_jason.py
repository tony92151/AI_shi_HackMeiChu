# In[3]:for picture
import numpy as np
import json

datanumber = 2

with open ('test.json','r',encoding="utf-8") as data:
    test=json.load(data)
    train_data = []
    label_data = []
    for i in range(datanumber):
        print("-----------------")
        print("location: train",test[i]["location"])
        print("fans_number:",test[i]["fans_number"])
        print("likes:",test[i]["likes"])
        print("comment_number",test[i]["comment_number"])
        train_data.append(test[i]["location"])
        print(int(test[i]["likes"])/int(test[i]["fans_number"]))
        if int(test[i]["likes"])/(test[i]["fans_number"]/1000) >29:
            a=np.eye(40,k=29)[0]
        else:
            a=np.eye(40,k=int(int(test[i]["likes"])/(int(test[i]["fans_number"])/1000))[0])
        if int(test[i]["comment_number"])/50 >9:
            b=np.eye(40,k=39)[0]
        else:
            b=np.eye(40,k=(int(int(test[i]["comment_number"])/50)+30))[0]
        train_data.append(a+b)
#print(train_data)
train_data_array = np.array(train_data)
train_data_array = train_data_array.reshape(datanumber,2)
print(train_data_array)
# In[3]:for picture
print(np.eye(40,k=39)[0])
# In[3]:for picture
import numpy as np
import json

datanumber = 2

with open ('test.json','r',encoding="utf-8") as data:
    test=json.load(data)
    train_data = []
    label_data = []

    for i in range(datanumber):
        #print("-----------------")
        #print("location: train",test[i]["location"])
        #print("fans_number:",test[i]["fans_number"])
        #print("likes:",test[i]["likes"])
        #print("comment_number",test[i]["comment_number"])
        train_data.append(test[i]["caption"])
        #train_data.append(test[i]["fans_number"])
        train_data.append(int(test[i]["likes"])/int(test[i]["fans_number"]))
        #train_data.append(test[i]["likes"])
        train_data.append(int(test[i]["comment_number"])/int(test[i]["fans_number"]))
        #train_data.append(test[i]["comment_number"])
#print(train_data)
train_data_array = np.array(train_data)
train_data_array = train_data_array.reshape(datanumber,2)
print(train_data_array)
    