// import Todo from '../views/todo/todo.vue'
// import Login from '../views/login/login.vue'

export default [
  {
    path: '/',
    redirect: '/app'
  },
  // {
  //   path: '/login/:id',
  //   component: Login,
  //   //props:true,
  //   // props:{
  //   //   id:1
  //   // },
  //   // props:(route)=>({id:route.query.cs1}),
  //   name: 'login',
  //   meta: {
  //     title: 'this is login',
  //     description: 'asdasd'
  //   },
  //   children: [
  //     {
  //       path: '/child1',
  //       component: Login-child1
  //     }
  //   ]
  // },
  {
    // path: '/app/:id', // /app/xxx
    path: '/app',
    props: true,
    // props: (route) => ({ id: route.query.b }),
    component: () => import(/* webpackChunkName: "todo-view" */ '../views/todo/todo.vue'),
    // component: Todo,
    name: 'app',
    meta: {
      title: 'this is app',
      description: 'asdasd'
    },
    beforeEnter (to, from, next) {
      console.log('app route before enter')
      next()
    }
    // children: [
    //   {
    //     path: 'test',
    //     component: Login
    //   }
    // ]
  },
  {
    path: '/login',
    component: () => import(/* webpackChunkName: "login-view" */ '../views/login/login.vue')
    // component: Login
  }
]
