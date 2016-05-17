//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.nl_component;

import edu.cmu.xprize.listener.ListenerBase;

public interface CNl_Processor {

    void setListener(ListenerBase   mListener);

    int getLength();
    String getString();
    int getValue();
    String getText();

    String deReference(String _listName, int index);

    void preProcessStimulus(String stimulusString);

    void listen(Boolean enable);

    void onUpdate(ListenerBase.HeardWord[] heardWords, boolean finalResult);
}