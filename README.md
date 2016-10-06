# JLiteSpider
**A lite distributed Java spider framework.**  
**这是一个轻量级的分布式java爬虫框架**

### 特点
这是一个强大，但又轻量级的分布式爬虫框架。jlitespider天生具有分布式的特点，各个worker之间需要通过一个或者多个消息队列来连接。消息队列我的选择是[rabbitmq](http://www.rabbitmq.com)。worker和消息之间可以是一对一，一对多，多对一或多对多的关系，这些都可以自由而又简单地配置。消息队列中存储的消息分为四种：url，页面源码，解析后的结果以及自定义的消息。同样的，worker的工作也分为四部分：下载页面，解析页面，数据持久化和自定义的操作。  
用户只需要在配置文件中，规定好worker和消息队列之间的关系。接着在代码中，定义好worker的四部分工作。即可完成爬虫的编写。

总体的使用流程如下：

* 启动rabbitmq。
* 在配置文件中定义worker和消息队列之间的关系。
* 在代码中编写worker的工作。
* 最后，启动爬虫。

### 安装

>使用maven：  

```xml
support later！
```

>直接下载jar包:  

点击[下载](http://7xrlnt.com1.z0.glb.clouddn.com/jlitespider.jar)。  

### Worker和消息队列之间关系

worker和消息队列之间的关系可以是一对一，多对一，一对多，多对多，都是可以配置的。在配置文件中，写上要监听的消息队列和要发送的消息队列。例如：

```json
{
    "workerid" : 2,
    "mq" : [{
        "name" : "one",
        "host" : "localhost",
        "port" : 5672,
        "qos" : 3  ,
        "queue" : "url"
    },
    {
        "name" : "two",
        "host" : "localhost",
        "port" : 5672,
        "qos" : 3  ,
        "queue" : "hello"
    }],
    "sendto" : ["two"],
    "recvfrom" : ["one", "two"]
}
```

>workerid : worker的id号  
>mq : 各个消息队列所在的位置，和配置信息。`name`字段为这个消息队列的唯一标识符，供消息队列的获取使用。`host`为消息队列所在的主机ip，`port`为消息队列的监听端口号（rabbitmq中默认为5672）。`qos`为消息队列每次将消息发给worker时的消息个数。`queue`为消息队列的名字。`host`+`port`+`queue`可以理解为是消息队列的唯一地址。  
>sendto : 要发送到的消息队列，填入的信息为`mq`中的`name`字段中的标识符。  
>recvfrom : 要监听的消息队列，消息队列会把消息分发到这个worker中。填入的信息同样为`mq`中的`name`字段中的标识符。

### 消息的设计

在消息队列中，消息一共有四种类型。分别是url，page，result和自定义类型。在worker的程序中，可以通过messagequeue的四种方法(sendUrl, sendPage, sendResult, send)来插入消息。worker的downloader会处理url消息，processor会处理page消息，saver会处理result消息，freeman会处理所有的自定义的消息。我们所要做的工作，就是实现好worker中的这四个函数。

### Worker接口的设计

JLiteSpider将整个的爬虫抓取流程抽象成四个部分，由四个接口来定义。分别是downloader，processor，saver和freeman。它们分别处理上述提到的四种消息。 

你所需要做的是，实现这个接口，并将想要抓取的url链表返回。具体的实现细节，可以由你高度定制。  

#### 1. Downloader:

>这部分实现的是页面下载的任务，将想要抓取的url链表，转化（下载后存储）为相应的页面数据链表。

接口设计如下：

```java
public interface Downloader {
	/**
	 * 下载url所指定的页面。
	 * @param url 
	 * 收到的由消息队列传过来的消息
	 * @param mQueue 
	 * 提供把消息发送到各个消息队列的方法
	 * @throws IOException
	 */
	public void download(Object url, Map<String, MessageQueue> mQueue) throws IOException;
}
```

你同样可以实现这个接口，具体的实现可由你自由定制，只要实现`download`函数。`url`是消息队列推送过来的消息，里面不一定是一条`url`，具体是什么内容，是由你当初传入消息队列时决定的。`mQueue`提供了消息发送到各个消息队列的方法，通过`mQueue.get("...")`选取消息队列，然后执行messagequeue的四种方法(sendUrl, sendPage, sendResult, send)来插入消息。

#### 2. Processor:

>`Processor`是解析器的接口，这里会从网页的原始文件中提取出有用的信息。

接口设计：

```java
public interface Processor{
	/**
	 * 处理下载下来的页面源代码
	 * @param page
	 * 消息队列推送过来的页面源代码数据消息
	 * @param mQueue
	 * 提供把消息发送到各个消息队列的方法
	 * @throws IOException
	 */
	public void process(Object page, Map<String, MessageQueue> mQueue) throws IOException;
}

```

实现这个接口，完成对页面源码的解析处理。`page`是由消息队列推送过来的消息，具体格式同样是由你在传入时决定好的。`mQueue`使用同上。  


#### 3. Saver:

>`Saver`实现的是对解析得到结果的处理，可以将你解析后得到的数据存入数据库，文件等等。或者将url重新存入消息队列，实现迭代抓取。

接口的设计：

```java
public interface Saver {
	/**
	 * 处理最终解析得到的结果
	 * @param result 
	 * 消息队列推送过来的结果消息
	 * @param mQueue 
	 * 提供把消息发送到各个消息队列的方法
	 * @throws IOException
	 */
	public void save(Object result, Map<String, MessageQueue> mQueue) throws IOException;
}
```

通过实现这个接口，可以完成对结果的处理。你同样可以实现这个接口，具体的实现可由你自由定制，只要实现`download`函数。`result`是消息队列推送过来的结果消息，具体的格式是由你当初传入消息队列时决定的。`mQueue`的使用同上。

#### 4. Freeman:

>通过上述的三个流程，可以实现爬虫抓取的一个正常流程。但是`jlitespider`同样提供了自定义的功能，你可以完善，加强，改进甚至颠覆上述的抓取流程。`freeman`就是一个处理自定义消息格式的接口，实现它就可以定义自己的格式，以至于定义自己的流程。

接口的设计：

```java
public interface Freeman {
	/**
	 * 自定义的处理函数
	 * @param key
	 * key为自定义的消息标记
	 * @param msg
	 * 消息队列推送的消息
	 * @param mQueue
	 * 提供把消息发送到各个消息队列的方法
	 * @throws IOException
	 */
	public void doSomeThing(String key, Object msg, Map<String, MessageQueue> mQueue) throws IOException;
}
```

通过实现`doSomeThing`函数，你就可以处理来自消息队列的自定义消息。`key`为消息的标记，`msg`为消息的内容。同样，通过`mQueue`的`send`方法，可以实现向消息队列发送自定义消息的操作。(需要注意，自定义的消息标记不能为：`url`，`page`，`result`。否则会被认为是`jlitespider`的保留消息，也就是由上述的三个接口函数来处理。)

### 总结说明

`jlitespider`的设计可能会让您有些疑惑，不过等您熟悉这一整套的设计之后，您就会发现`jlitespider`是多么的灵活和易于使用。

###使用方法

JLiteSpider使用：

```java
//worker的启动
Spider.create() //创建实例
      .setDownloader(...) //设置实现了Downloader接口的下载器
      .setProcessor(...) //设置实现了Processor接口的解析器
      .setSaver(...) //设置实现了Saver接口的数据持久化方法
      .setFreeman(...) //设置自定义消息的处理函数
      .setSettingFile(...) //设置配置文件
      .begin(); //开始爬虫

//消息队列中初始消息添加器的使用
MessageQueueAdder.create("localhost", 5672, "url")
                 .addUrl(...) //向消息队列添加url类型的消息
                 .addPage(...) //向消息队列添加page类型的消息
                 .addResult(...) //向消息队列添加result类型的消息
                 .add(..., ...) //向消息队列添加自定义类型的消息
                 .close() //关闭连接，一定要记得在最后调用！
```

以豆瓣电影的页面为例子：  

```java
continue ... 
```

### 辅助工具

>当前版本的`jlitespider`能提供的辅助工具并不多，您在使用`jlitespider`的过程中，可以将您实现的辅助工具合并到`jlitespider`中来，一起来完善`jlitespider`的功能。辅助工具在包`com.github.luohaha.jlitespider.extension`中。

1. Network

简单的网络下载器，输入url，返回页面源代码。使用如下：

```java
		String result = Network.create()
				.setCookie("...")
				.setProxy("...")
				.setTimeout(...)
				.setUserAgent("...")
				.downloader(url);
```

