/*
    Copyright (c) 2012-2013, BogDan Vatra <bogdan@kde.org>
    Contact: http://www.qt.io/licensing/

    Commercial License Usage
    Licensees holding valid commercial Qt licenses may use this file in
    accordance with the commercial license agreement provided with the
    Software or, alternatively, in accordance with the terms contained in
    a written agreement between you and The Qt Company. For licensing terms
    and conditions see http://www.qt.io/terms-conditions. For further
    information use the contact form at http://www.qt.io/contact-us.

    BSD License Usage
    Alternatively, this file may be used under the BSD license as follows:
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.qtproject.qt5.android.bindings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;
import android.util.Log;

// ============================================================
// ★ ★ ★ SHOWLOG ★ ★ ★
// ============================================================
import org.kivy.android.PythonService;

public class QtApplication extends Application
{
    public final static String QtTAG = "Qt";
    public static Object m_delegateObject = null;
    public static HashMap<String, ArrayList<Method>> m_delegateMethods= new HashMap<String, ArrayList<Method>>();
    public static Method dispatchKeyEvent = null;
    public static Method dispatchPopulateAccessibilityEvent = null;
    public static Method dispatchTouchEvent = null;
    public static Method dispatchTrackballEvent = null;
    public static Method onKeyDown = null;
    public static Method onKeyMultiple = null;
    public static Method onKeyUp = null;
    public static Method onTouchEvent = null;
    public static Method onTrackballEvent = null;
    public static Method onActivityResult = null;
    public static Method onCreate = null;
    public static Method onKeyLongPress = null;
    public static Method dispatchKeyShortcutEvent = null;
    public static Method onKeyShortcut = null;
    public static Method dispatchGenericMotionEvent = null;
    public static Method onGenericMotionEvent = null;

    // ============================================================
    // ★ ★ ★ SHOWLOG ★ ★ ★
    // ============================================================
    private static void showLog(String title, String message) {
        // 1. Android Log
        Log.d("QtApplication", title + ": " + message);
        
        // 2. PythonService.showLog verwenden (für einheitliche Logs)
        PythonService.showLog("QtApplication", title + ": " + message);
    }

    public static void setQtActivityDelegate(Object listener)
    {
        showLog("setQtActivityDelegate", "📝 Setze QtActivity Delegate");
        
        QtApplication.m_delegateObject = listener;
        showLog("setQtActivityDelegate", "✅ Delegate gesetzt: " + listener.getClass().getName());

        ArrayList<Method> delegateMethods = new ArrayList<Method>();
        for (Method m : listener.getClass().getMethods()) {
            if (m.getDeclaringClass().getName().startsWith("org.qtproject.qt5.android")) {
                delegateMethods.add(m);
                showLog("setQtActivityDelegate", "📌 Methode gefunden: " + m.getName());
            }
        }
        showLog("setQtActivityDelegate", "📋 " + delegateMethods.size() + " Delegate-Methoden gefunden");

        ArrayList<Field> applicationFields = new ArrayList<Field>();
        for (Field f : QtApplication.class.getFields()) {
            if (f.getDeclaringClass().getName().equals(QtApplication.class.getName())) {
                applicationFields.add(f);
                showLog("setQtActivityDelegate", "📌 Feld gefunden: " + f.getName());
            }
        }

        for (Method delegateMethod : delegateMethods) {
            try {
                QtActivity.class.getDeclaredMethod(delegateMethod.getName(), delegateMethod.getParameterTypes());
                if (QtApplication.m_delegateMethods.containsKey(delegateMethod.getName())) {
                    QtApplication.m_delegateMethods.get(delegateMethod.getName()).add(delegateMethod);
                    showLog("setQtActivityDelegate", "➕ Methode hinzugefügt: " + delegateMethod.getName());
                } else {
                    ArrayList<Method> delegateSet = new ArrayList<Method>();
                    delegateSet.add(delegateMethod);
                    QtApplication.m_delegateMethods.put(delegateMethod.getName(), delegateSet);
                    showLog("setQtActivityDelegate", "✅ Neue Methode registriert: " + delegateMethod.getName());
                }
                for (Field applicationField:applicationFields) {
                    if (applicationField.getName().equals(delegateMethod.getName())) {
                        try {
                            applicationField.set(null, delegateMethod);
                            showLog("setQtActivityDelegate", "🔗 Feld gesetzt: " + applicationField.getName());
                        } catch (Exception e) {
                            showLog("setQtActivityDelegate", "❌ Fehler beim Setzen von " + applicationField.getName() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                showLog("setQtActivityDelegate", "⚠️ Fehler bei Methode " + delegateMethod.getName() + ": " + e.getMessage());
            }
        }
        
        showLog("setQtActivityDelegate", "✅ Delegate Setup abgeschlossen");
    }

    @Override
    public void onTerminate() {
        showLog("onTerminate", "🗑️ QtApplication onTerminate");
        
        if (m_delegateObject != null && m_delegateMethods.containsKey("onTerminate")) {
            showLog("onTerminate", "📝 Rufe onTerminate Delegate auf");
            invokeDelegateMethod(m_delegateMethods.get("onTerminate").get(0));
        }
        super.onTerminate();
        showLog("onTerminate", "✅ onTerminate abgeschlossen");
    }

    public static class InvokeResult
    {
        public boolean invoked = false;
        public Object methodReturns = null;
    }

    private static int stackDeep = -1;
    
    public static InvokeResult invokeDelegate(Object... args)
    {
        InvokeResult result = new InvokeResult();
        if (m_delegateObject == null) {
            showLog("invokeDelegate", "⚠️ Kein Delegate-Objekt");
            return result;
        }
        
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (-1 == stackDeep) {
            String activityClassName = QtActivity.class.getCanonicalName();
            for (int it=0; it<elements.length; it++) {
                if (elements[it].getClassName().equals(activityClassName)) {
                    stackDeep = it;
                    showLog("invokeDelegate", "🔍 StackDeep gefunden: " + stackDeep);
                    break;
                }
            }
        }
        
        if (-1 == stackDeep) {
            showLog("invokeDelegate", "⚠️ StackDeep nicht gefunden");
            return result;
        }
        
        final String methodName = elements[stackDeep].getMethodName();
        if (!m_delegateMethods.containsKey(methodName)) {
            showLog("invokeDelegate", "⏭️ Keine Delegate-Methode für: " + methodName);
            return result;
        }

        showLog("invokeDelegate", "📝 Rufe Delegate auf: " + methodName + " mit " + args.length + " Argumenten");
        
        for (Method m : m_delegateMethods.get(methodName)) {
            if (m.getParameterTypes().length == args.length) {
                result.methodReturns = invokeDelegateMethod(m, args);
                result.invoked = true;
                showLog("invokeDelegate", "✅ Delegate erfolgreich: " + methodName);
                return result;
            }
        }
        
        showLog("invokeDelegate", "⚠️ Keine passende Methode für: " + methodName);
        return result;
    }

    public static Object invokeDelegateMethod(Method m, Object... args)
    {
        try {
            showLog("invokeDelegateMethod", "📞 Rufe auf: " + m.getName());
            Object result = m.invoke(m_delegateObject, args);
            showLog("invokeDelegateMethod", "✅ Aufruf erfolgreich: " + m.getName());
            return result;
        } catch (Exception e) {
            showLog("invokeDelegateMethod", "❌ Fehler bei " + m.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}