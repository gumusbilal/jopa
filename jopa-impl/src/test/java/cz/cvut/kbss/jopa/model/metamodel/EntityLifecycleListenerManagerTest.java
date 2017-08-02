package cz.cvut.kbss.jopa.model.metamodel;

import cz.cvut.kbss.jopa.environment.Vocabulary;
import cz.cvut.kbss.jopa.model.annotations.EntityListeners;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.PostLoad;
import cz.cvut.kbss.jopa.model.annotations.PrePersist;
import cz.cvut.kbss.jopa.model.lifecycle.LifecycleEvent;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class EntityLifecycleListenerManagerTest {

    private EntityLifecycleListenerManager manager = new EntityLifecycleListenerManager();

    @Test
    public void listenerInvocationInvokesCorrectCallback() throws Exception {
        manager.addLifecycleCallback(LifecycleEvent.PRE_PERSIST, Child.class.getDeclaredMethod("prePersistChild"));
        final Child instance = spy(new Child());
        manager.invokePrePersistCallbacks(instance);
        verify(instance).prePersistChild();
    }

    @Test
    public void listenerInvocationDoesNothingWhenNoMatchingListenerExists() throws Exception {
        // The callback is not registered
        final Child instance = spy(new Child());
        manager.invokePrePersistCallbacks(instance);
        verify(instance, never()).prePersistChild();
    }

    @Test
    public void listenerInvocationInvokesListenersTopDown() throws Exception {
        final EntityLifecycleListenerManager parentManager = new EntityLifecycleListenerManager();
        parentManager.addLifecycleCallback(LifecycleEvent.PRE_PERSIST, Parent.class.getDeclaredMethod("prePersist"));
        manager.setParent(parentManager);
        manager.addLifecycleCallback(LifecycleEvent.PRE_PERSIST, Child.class.getDeclaredMethod("prePersistChild"));
        final Child instance = spy(new Child());
        manager.invokePrePersistCallbacks(instance);
        final InOrder inOrder = inOrder(instance);
        inOrder.verify(instance).prePersist();
        inOrder.verify(instance).prePersistChild();
    }

    @Test
    public void listenerInvocationInvokesAncestorListenersWhenNoneAreDeclaredDirectlyOnEntity() throws Exception {
        final EntityLifecycleListenerManager parentManager = new EntityLifecycleListenerManager();
        parentManager.addLifecycleCallback(LifecycleEvent.PRE_PERSIST, Parent.class.getDeclaredMethod("prePersist"));
        manager.setParent(parentManager);
        final Child instance = spy(new Child());
        manager.invokePrePersistCallbacks(instance);
        verify(instance).prePersist();
        verify(instance, never()).prePersistChild();
    }

    @SuppressWarnings("unused")
    private static class ParentListener {
        @PostLoad
        void postLoad(Parent instance) {
        }
    }

    @SuppressWarnings("unused")
    private static class ChildListener {
        @PostLoad
        void postLoad(Object instance) {
        }
    }

    @SuppressWarnings("unused")
    private static class AnotherChildListener {
        @PostLoad
        void postLoad(Child instance) {
        }
    }

    @EntityListeners(ParentListener.class)
    @OWLClass(iri = Vocabulary.CLASS_BASE + "Parent")
    private static class Parent {

        @PrePersist
        void prePersist() {
        }

        @PostLoad
        void postLoad() {
        }
    }

    @EntityListeners({ChildListener.class, AnotherChildListener.class})
    @OWLClass(iri = Vocabulary.CLASS_BASE + "Child")
    private static class Child extends Parent {

        @PrePersist
        void prePersistChild() {
        }

        @PostLoad
        void postLoadChild() {
        }
    }

    @Test
    public void listenerInvocationInvokesEntityListenerCallbacks() throws Exception {
        final ParentListener listener = spy(new ParentListener());
        manager.addEntityListener(listener);
        manager.addEntityListenerCallback(listener, LifecycleEvent.POST_LOAD,
                ParentListener.class.getDeclaredMethod("postLoad", Parent.class));
        final Parent instance = new Parent();
        manager.invokePostLoadCallbacks(instance);
        verify(listener).postLoad(instance);
    }

    @Test
    public void listenerInvocationInvokesEntityListenerCallbacksTopDown() throws Exception {
        final EntityLifecycleListenerManager parentManager = new EntityLifecycleListenerManager();
        final ParentListener parentListener = spy(new ParentListener());
        parentManager.addEntityListener(parentListener);
        parentManager.addEntityListenerCallback(parentListener, LifecycleEvent.POST_LOAD,
                ParentListener.class.getDeclaredMethod("postLoad", Parent.class));
        manager.setParent(parentManager);
        final ChildListener childListener = spy(new ChildListener());
        manager.addEntityListener(childListener);
        manager.addEntityListenerCallback(childListener, LifecycleEvent.POST_LOAD,
                ChildListener.class.getDeclaredMethod("postLoad", Object.class));
        final Child instance = new Child();
        manager.invokePostLoadCallbacks(instance);
        final InOrder inOrder = inOrder(parentListener, childListener);
        inOrder.verify(parentListener).postLoad(instance);
        inOrder.verify(childListener).postLoad(instance);
    }

    @Test
    public void listenerInvocationInvokesEntityListenersInOrderOfDeclarationOnEntity() throws Exception {
        final ChildListener childListener = spy(new ChildListener());
        manager.addEntityListener(childListener);
        manager.addEntityListenerCallback(childListener, LifecycleEvent.POST_LOAD,
                ChildListener.class.getDeclaredMethod("postLoad", Object.class));
        final AnotherChildListener anotherChildListener = spy(new AnotherChildListener());
        manager.addEntityListener(anotherChildListener);
        manager.addEntityListenerCallback(anotherChildListener, LifecycleEvent.POST_LOAD,
                AnotherChildListener.class.getDeclaredMethod("postLoad", Child.class));
        final Child instance = new Child();
        manager.invokePostLoadCallbacks(instance);

        final InOrder inOrder = inOrder(childListener, anotherChildListener);
        inOrder.verify(childListener).postLoad(instance);
        inOrder.verify(anotherChildListener).postLoad(instance);
    }

    @Test
    public void listenerInvocationInvokesEntityListenerCallbacksBeforeInternalLifecycleCallbacks() throws Exception {
        final EntityLifecycleListenerManager parentManager = new EntityLifecycleListenerManager();
        final ParentListener parentListener = spy(new ParentListener());
        parentManager.addEntityListener(parentListener);
        parentManager.addEntityListenerCallback(parentListener, LifecycleEvent.POST_LOAD,
                ParentListener.class.getDeclaredMethod("postLoad", Parent.class));
        parentManager.addLifecycleCallback(LifecycleEvent.POST_LOAD, Parent.class.getDeclaredMethod("postLoad"));
        manager.setParent(parentManager);
        manager.setParent(parentManager);
        final ChildListener childListener = spy(new ChildListener());
        manager.addEntityListener(childListener);
        manager.addEntityListenerCallback(childListener, LifecycleEvent.POST_LOAD,
                ChildListener.class.getDeclaredMethod("postLoad", Object.class));
        manager.addLifecycleCallback(LifecycleEvent.POST_LOAD, Child.class.getDeclaredMethod("postLoadChild"));
        final Child instance = spy(new Child());
        manager.invokePostLoadCallbacks(instance);

        final InOrder inOrder = inOrder(parentListener, childListener, instance);
        inOrder.verify(parentListener).postLoad(instance);
        inOrder.verify(childListener).postLoad(instance);
        inOrder.verify(instance).postLoad();
        inOrder.verify(instance).postLoadChild();
    }
}