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

package cmu.xprize.robotutor.tutorengine.widgets.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import cmu.xprize.robotutor.tutorengine.CTutor;
import cmu.xprize.robotutor.tutorengine.CTutorSceneDelegate;
import cmu.xprize.robotutor.tutorengine.ITutorLogManager;
import cmu.xprize.robotutor.tutorengine.ITutorNavigator;
import cmu.xprize.robotutor.tutorengine.ITutorSceneImpl;
import cmu.xprize.robotutor.tutorengine.graph.scene_descriptor;


public class TSceneRelativeLayout extends RelativeLayout implements ITutorSceneImpl {

    private CTutorSceneDelegate mTutorScene;

    final private String TAG = "CSceneFrameLayout";


    public TSceneRelativeLayout(Context context) {
        super(context);

        init(context, null);
    }

    public TSceneRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public TSceneRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {
        mTutorScene = new CTutorSceneDelegate(this);
        mTutorScene.init(context, attrs);
    }

    @Override
    public void setName(String name) {
        mTutorScene.setName(name);
    }

    @Override
    public String name() {
        return mTutorScene.name();
    }

    @Override
    public void setParent(ITutorSceneImpl mParent) {
        mTutorScene.setParent(mParent);
    }

    @Override
    public void setTutor(CTutor tutor) {
        mTutorScene.setTutor(tutor);
    }

    @Override
    public void setNavigator(ITutorNavigator navigator) {
        mTutorScene.setNavigator(navigator);
    }

    @Override
    public void setLogManager(ITutorLogManager logManager) {
        mTutorScene.setLogManager(logManager);
    }


    @Override
    public CTutorSceneDelegate getimpl() {
        return mTutorScene;
    }


    @Override
    public ViewGroup getOwner() {
        return mTutorScene.getOwner();
    }

    @Override
    public String preEnterScene(scene_descriptor scene, String Direction) {
        return mTutorScene.preEnterScene(scene, Direction);
    }

    @Override
    public void onEnterScene() {
        mTutorScene.onEnterScene();
    }

    @Override
    public String preExitScene(String Direction, int sceneCurr) {
        return mTutorScene.preExitScene(Direction, sceneCurr);
    }

    @Override
    public void onExitScene() {
        mTutorScene.onExitScene();
    }
}