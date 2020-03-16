//vue-loader的相关配置
module.exports = (isDev) => {
  return {
    preserveWhitepace: true,//去掉空格
    extractCSS: !isDev,//同意在打包的时候，将.vue中的css打包到ExtractPlugin指定的单独css文件中
    //css模块化的配置
    cssModules: {
      //[hash:base64:5]：css文件内容的hash码的base64字符串截取5个字符
      localIdentName: isDev ? '[path]-[name]-[hash:base64:5]' : '[hash:base64:5]',//生成文件路径+文件名类+[hash:base64:5]的名字
      camelCase: true //将css中类似space-white这种横岗连接的类名在js中转为spaceWhite这种的驼峰式，方便js调用
    },
    // hotReload: false, // 根据环境变量生成
    //loaders和preLoader都是json格式，key是文件类型，value是这种文件类型使用哪种loader来解析
    //loaders: {} 指定loader
    //preLoader: {} //经过loaders解析的文件再使用preLoaders中定义的来解析，key要对应
    //postLoader: {} //经过preLoaders解析后，再使用
  }
}
